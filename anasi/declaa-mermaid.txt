classDiagram
    direction LR

    class Building {
        -Integer number
        +getNumber() Integer
        +setNumber(Integer number) void
    }

    class Room {
        -Integer number
        -Building building
        +getNumber() Integer
        +setNumber(Integer number) void
        +getBuilding() Building
        +setBuilding(Building building) void
    }

    class Material {
        -String serialNumber
        -EMaterialType materialType
        -LocalDate endAvailabilityDate
        -Room room
        +getSerialNumber() String
        +setSerialNumber(String serialNumber) void
        +getMaterialType() EMaterialType
        +setMaterialType(EMaterialType materialType) void
        +getEndAvailabilityDate() LocalDate
        +setEndAvailabilityDate(LocalDate endAvailabilityDate) void
        +getRoom() Room
        +setRoom(Room room) void
    }

    class EMaterialType {
        <<enumeration>>
        UC
        SCREEN
        PROJECTOR
        PERIPHERAL
        -String libelle
        +getLibelle() String
    }

    Room "1" --> "1" Building : building
    Material "*" --> "0..1" Room : room
    Material --> EMaterialType : materialType