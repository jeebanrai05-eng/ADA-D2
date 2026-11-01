# ADA v2 Firestore Security Specification

## 1. Data Invariants
- Every document in `/users/{userId}/memories/{memoryId}`, `/users/{userId}/cad_models/{modelId}`, `/users/{userId}/smart_devices/{deviceId}`, and `/users/{userId}/printers/{printerId}` belongs exclusively to `userId`.
- Unauthenticated callers (`request.auth == null`) cannot read, list, create, update, or delete any document.
- Cross-user access (`request.auth.uid != userId` or `resource.data.userId != request.auth.uid`) is strictly rejected.
- `userId` and `createdAt` are immutable across all updates.
- All timestamps (`createdAt`, `updatedAt`) must be valid Firestore `timestamp` objects `<= request.time`.
- All strings and numeric fields enforce strict bounds and enum allowlists.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/memories").get()` -> REJECTED.
2. **Cross-User Read**: Bob reading `/users/alice/memories/m1` -> REJECTED.
3. **Identity Spoofing on Create**: Alice writing `userId: "bob"` inside `/users/alice/memories/m1` -> REJECTED.
4. **Shadow/Ghost Field Injection**: Alice writing `isAdmin: true` to `/users/alice/memories/m1` -> REJECTED by `hasOnly()`.
5. **Invalid Enum Value**: Alice writing `category: "hacked"` to `/users/alice/memories/m1` -> REJECTED.
6. **Oversized String**: Alice writing `content` > 2000 chars to `/users/alice/memories/m1` -> REJECTED.
7. **Out-of-Bounds Integer**: Alice writing `importance: 99` to `/users/alice/memories/m1` -> REJECTED.
8. **Future Timestamp Spoofing**: Alice writing `createdAt` in the future (`> request.time`) -> REJECTED.
9. **Immutable Field Mutation**: Alice changing `createdAt` or `userId` during an update -> REJECTED.
10. **Invalid CAD Dimensions**: Alice writing `paramRadius: -10` or `shapeType: "unknown"` -> REJECTED.
11. **Invalid Smart Device Brightness**: Alice writing `brightness: 150` to `/users/alice/smart_devices/d1` -> REJECTED.
12. **Invalid Printer Thermal Target**: Alice writing `nozzleTarget: 900` to `/users/alice/printers/p1` -> REJECTED.
