classDiagram
    direction TB

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
        -boolean archived
        +getSerialNumber() String
        +setSerialNumber(String serialNumber) void
        +getMaterialType() EMaterialType
        +setMaterialType(EMaterialType materialType) void
        +getEndAvailabilityDate() LocalDate
        +setEndAvailabilityDate(LocalDate endAvailabilityDate) void
        +getRoom() Room
        +setRoom(Room room) void
        +isArchived() boolean
        +setArchived(boolean archived) void
    }

    class User {
        -String username
        -ERole role
        -boolean enabled
        +getUsername() String
        +setUsername(String username) void
        +getRole() ERole
        +setRole(ERole role) void
        +isEnabled() boolean
        +setEnabled(boolean enabled) void
        +getAuthorities() Collection
    }

    class Application {
        -String name
        -String displayName
        -String iconBase64
        -boolean blacklisted
        -String description
        +getName() String
        +setName(String name) void
        +getDisplayName() String
        +setDisplayName(String displayName) void
        +getIconBase64() String
        +setIconBase64(String iconBase64) void
        +isBlacklisted() boolean
        +setBlacklisted(boolean blacklisted) void
        +getDescription() String
        +setDescription(String description) void
    }

    class Report {
        -String description
        -LocalDateTime reportDateTime
        -User user
        -Material material
        -LocalDateTime handleDateTime
        -boolean handled
        +getDescription() String
        +setDescription(String description) void
        +getReportDateTime() LocalDateTime
        +setReportDateTime(LocalDateTime reportDateTime) void
        +getUser() User
        +setUser(User user) void
        +getMaterial() Material
        +setMaterial(Material material) void
        +getHandleDateTime() LocalDateTime
        +setHandleDateTime(LocalDateTime handleDateTime) void
        +isHandled() boolean
        +setHandled(boolean handled) void
    }

    class KillEvent {
        -String processName
        -Material uc
        -User user
        -LocalDateTime killDateTime
        -Boolean result
        +getProcessName() String
        +setProcessName(String processName) void
        +getUc() Material
        +setUc(Material uc) void
        +getUser() User
        +setUser(User user) void
        +getKillDateTime() LocalDateTime
        +setKillDateTime(LocalDateTime killDateTime) void
        +getResult() Boolean
        +setResult(Boolean result) void
        +isResult() Boolean
    }

    class EMaterialType {
        <<enumeration>>
        UC
        SCREEN
        PROJECTOR
        PERIPHERAL
    }

    class ERole {
        <<enumeration>>
        USER
        MANAGER
        ADMIN
        -Set~ERole~ parents
        +getParents() Set~ERole~
        +getAllRoles() Set~ERole~
        +hasRole(ERole role) boolean
    }

    %% Associations
    Room "*" --> "1" Building : building
    Material "*" --> "0..1" Room : room
    Material --> "1" EMaterialType : materialType
    User --> "1" ERole : role
    Report "*" --> "1" User : user
    Report "*" --> "1" Material : material
    KillEvent "*" --> "1" Material : uc
    KillEvent "*" --> "1" User : user
