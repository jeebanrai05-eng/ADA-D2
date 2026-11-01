const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read memories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/memories`).get());
});

test("Authenticated user: can create, read, update, and list own memories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1");

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      category: "project",
      key: "Mark VII Armor",
      content: "Primary HUD telemetry and titanium-gold alloy specs",
      importance: 5,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    docRef.update({
      content: "Updated Mark VII Armor telemetry specs",
      importance: 4,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access: Bob cannot read or write Alice's memories", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("memories").doc("mem_secret").set({
      userId: ALICE_UID,
      category: "personal",
      key: "Workshop Code",
      content: "Protocol 42",
      importance: 5,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_secret").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_hack").set({
      userId: BOB_UID,
      category: "personal",
      key: "Hack",
      content: "Injected",
      importance: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Shadow update & invalid enum rejected on memories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_bad");

  // Ghost field isAdmin
  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      category: "fact",
      key: "Key",
      content: "Content",
      importance: 3,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid category enum
  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      category: "invalid_cat",
      key: "Key",
      content: "Content",
      importance: 3,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and query CAD models, smart devices, and printers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cad_models").doc("cad_1").set({
      userId: ALICE_UID,
      name: "Planetary Gear M3",
      prompt: "Create a 24-tooth planetary gear with 40mm radius",
      shapeType: "gear",
      scriptCode: "from build123d import *\nwith BuildPart() as p:\n    Cylinder(40, 12)",
      dimensionsSummary: "80mm x 80mm x 12mm",
      paramRadius: 40,
      paramHeight: 12,
      paramSegments: 24,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("smart_devices").doc("dev_1").set({
      userId: ALICE_UID,
      name: "Workshop Main Array",
      room: "Lab Alpha",
      deviceType: "strip",
      ipAddress: "192.168.1.104",
      isOn: true,
      brightness: 85,
      powerWatts: 42,
      colorHex: "#00E5FF",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("printers").doc("prt_1").set({
      userId: ALICE_UID,
      name: "Voron 2.4 CoreXY",
      hostUrl: "http://192.168.1.150:7125",
      status: "printing",
      activeJobName: "arc_reactor_housing_v2.gcode",
      progressPercent: 64,
      nozzleTemp: 245,
      nozzleTarget: 245,
      bedTemp: 105,
      bedTarget: 105,
      createdAt: now,
      updatedAt: now,
    })
  );
});
