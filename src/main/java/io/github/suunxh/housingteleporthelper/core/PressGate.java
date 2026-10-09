package io.github.suunxh.housingteleporthelper.core;

/** Physical rising edges; still records held keys while a GUI or missing world blocks actions. */
public final class PressGate {
    private boolean targetHeld, plateHeld;
    private int previousTargetCode = Integer.MIN_VALUE, previousPlateCode = Integer.MIN_VALUE;
    public static final class Presses {
        public final boolean target, plate, conflict;
        private Presses(boolean target, boolean plate, boolean conflict) {
            this.target = target; this.plate = plate; this.conflict = conflict;
        }
    }
    public Presses update(int targetCode, int plateCode, boolean targetDown, boolean plateDown, boolean enabled) {
        boolean targetChanged = previousTargetCode != Integer.MIN_VALUE && targetCode != previousTargetCode;
        boolean plateChanged = previousPlateCode != Integer.MIN_VALUE && plateCode != previousPlateCode;
        boolean target = enabled && !targetChanged && targetDown && !targetHeld;
        boolean plate = enabled && !plateChanged && plateDown && !plateHeld;
        targetHeld = targetDown; plateHeld = plateDown;
        previousTargetCode = targetCode; previousPlateCode = plateCode;
        boolean conflict = targetCode != 0 && targetCode == plateCode && (target || plate);
        // Do not guess which action the user intended when both binds use one key.
        return new Presses(!conflict && target, !conflict && plate, conflict);
    }
}
