package com.gavin101;


import org.powbot.api.*;
import org.powbot.api.rt4.*;
import org.powbot.api.rt4.walking.model.Skill;
import org.powbot.api.script.AbstractScript;
import org.powbot.api.script.ScriptManifest;
import org.powbot.api.script.paint.Paint;
import org.powbot.api.script.paint.PaintBuilder;
import org.powbot.mobile.service.ScriptUploader;


import java.util.List;

@ScriptManifest(
        name = "CakeThiever",
        description = "Steals cakes from the Ardy baker stall. Good for new ironman accounts.",
        version = "0.0.1"
)


public class CakeThiever extends AbstractScript {

    String currentState = "Starting";
    Tile THIEVING_TILE = new Tile(2669, 3310, 0);
    Tile bankTopLeft = new Tile(2652, 3286, 0);
    Tile bankTopRight = new Tile(2655, 3281, 0);
    Area BANK_AREA = new Area(bankTopLeft, bankTopRight);
//    Tile BANK_TILE = new Tile(2655, 3286, 0);
    static final int STALL_ID = 11730;
    static final int WALK_ANIM = 819;
    static final int CAKE_ID = 1891;
    public static final String[] badItems = {"Chocolate slice", "Bread"};
    List<Item> itemsToDrop = Inventory.stream().name(badItems).list();

    public static void main(String[] args) {
        new ScriptUploader().uploadAndStart("CakeThiever", "hcim", "powbot", false, true);
    }

    @Override
    public void poll() {
        if (shouldDropItems()) {
            dropItems();
        }
        else if (shouldThieve()) {
            stealCake();
        } else if (shouldBank()) {
            depositCakes();
        }
    }

    @Override
    public void onStart() {
        Condition.sleep(1000);
        System.out.println("Starting Gavin's Ardy Cake Thiever");

        Paint paint = new PaintBuilder().trackSkill(Skill.Thieving)
                .trackInventoryItem(CAKE_ID)
                .addString(() -> currentState)
                .build();
        addPaint(paint);
    }


    public boolean shouldDropItems() {
        state("Checking if we have non-full cakes to drop");
        List<Item> itemsToDrop = Inventory.stream().name(badItems).list();
        return !itemsToDrop.isEmpty();
    }

    public void dropItems() {
        state("Dropping non cakes");
        List<Item> itemsToDrop = Inventory.stream().name(badItems).list();
        Inventory.drop(itemsToDrop);
        Condition.wait(() -> itemsToDrop.isEmpty(), 20, 50);
    }

    public boolean shouldThieve() {
        return !Inventory.isFull();
    }

    public void stealCake() {
        state("Entering stealCake()"); // debug
        if (!Game.tab(Game.Tab.INVENTORY)) {
            Condition.wait(() -> Game.tab(Game.Tab.INVENTORY), 250, 10);
        }
        if (!Players.local().tile().equals(THIEVING_TILE) && Players.local().movementAnimation() < WALK_ANIM) { // Need to move to our thieving spot (Should never get caught by a guard here.)
            state("Walking to Thieving spot");
            Movement.walkTo(THIEVING_TILE);
            Condition.wait(() -> Players.local().tile() == THIEVING_TILE, 150, 20);
        } else if (Players.local().animation() == -1) { // Not currently thieving
            GameObject cakeStall = Objects.stream().within(2).id(STALL_ID).nearest().first();
            if (cakeStall.valid()) {
                if (!cakeStall.inViewport()) { // Need to turn camera to the stall
                    state("Turning camera to cake stall");
                    Camera.turnTo(cakeStall);
                    Condition.wait(() -> cakeStall.inViewport(), 250, 10);
                } else { // Cake stall isn't null and in view
                    state("Stealing cake from stall");
                    cakeStall.interact("Steal-from");
                    Condition.wait(() -> !cakeStall.valid(), 150, 50); // Turns into "market stall" (id:634) after you steal from it
                }
            } else {
                state("Waiting for stall to restock");
            }
        }
    }

    public boolean shouldBank() {
        return Inventory.isFull() && Inventory.stream().name(badItems).list().isEmpty();
    }

    public void depositCakes() {
        if (!BANK_AREA.contains(Players.local().tile()) && Players.local().movementAnimation() < WALK_ANIM) {
            state("Walking to bank");
            Movement.walkTo(BANK_AREA.getRandomTile());
            Condition.wait(() -> BANK_AREA.contains(Players.local().tile()), 150, 20);
        } else if (!Bank.opened()) {
            // Open bank once we're at the bank tile
            state("Opening bank");
            Condition.wait(() -> Bank.open(), 150, 20);
        } else {
            // Deposit all items in inventory
            state("Depositing cakes into bank");
            Bank.depositInventory();
            Condition.wait(() -> Inventory.isEmpty(), 150, 20);
            // Close bank
            state("Closing bank");
            Bank.close();
            Condition.wait(() -> !Bank.opened(), 150, 20);
        }
    }

    public void state(String s) {
        currentState = s;
        System.out.println(s);
    }
}
