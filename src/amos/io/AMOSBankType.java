package amos.io;

public enum AMOSBankType {
    UNKNOWN(""),
    SPRITEBANK("AmSp"), 
    ICONBANK("AmIc"),
    MEMORYBANK("AmBk"),
    // The definitions below are actually subtypes of MEMORYBANK
    PACKED_PICTURE("Pac.Pic."),
    TRACKER("Tracker "),
    SAMPLES("Samples ");

    private final String _idString;
    
    AMOSBankType(String idString) {
        this._idString = idString;
    }

    public static AMOSBankType GetAMOSBankTypeById(String idString) {
        for (AMOSBankType bankType : AMOSBankType.values()) {
            if (idString.equals(bankType._idString)) return bankType;
        }
        return UNKNOWN;
    }  

}
