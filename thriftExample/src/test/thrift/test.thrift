namespace * com.gu.marley


struct SubUnion1 {
  1: required string id;
}

struct SubUnion2 {
  1: required string id;
}

union ExampleUnion {
  1: SubUnion1 subunion1;
  2: SubUnion2 subunion2;
}

enum ExampleEnum {
  A1 = 0;
  B2 = 1;
  C3 = 2;
}

struct SubStruct {
  1: required bool falseOrTrue;
}

struct ExampleStruct {
  1: required string requiredString;
  2: optional string optString;
  3: required ExampleEnum requiredEnum;
  4: optional bool defaultBool = false;
  5: required double requiredDouble;
  6: optional list<string> optListString;
  7: required SubStruct subStruct;
  8: optional set<string> stringSet;
  9: optional i32 optInt;
  10: optional i16 optShort;
  11: optional i64 optLong;
  12: optional byte optByte;
  13: optional map<string, i32> stringIntMap;
  14: required ExampleUnion exampleUnion;
}
