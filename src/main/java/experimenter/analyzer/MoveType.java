package experimenter.analyzer;

class MoveType {
    private final String identifier;
    private int num;
    private double sizeAverage;

    public MoveType(MoveInfo m) {
        this.identifier = m.getIdentifier();
        this.num = 1;
        this.sizeAverage = m.getSize();
    }

    public void add(MoveType m) {
        this.sizeAverage = (sizeAverage * num + m.sizeAverage) / (num + 1);
        this.num += 1;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getNum() {
        return num;
    }

    public double getSizeAverage() {
        return sizeAverage;
    }

    public int hashCode() {
        return identifier.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof MoveType) {
            MoveType moveType = (MoveType)obj;
            return this.identifier.equals(moveType.identifier);
        } else
            return false;
    }
}