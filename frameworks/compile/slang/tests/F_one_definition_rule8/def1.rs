#pragma version(1)
#pragma rs java_package_name(foo)

typedef int Type1;
typedef struct DifferentDefinition7{
	Type1 member1;
} DifferentDefinition7;

// expected-error: two level
typedef struct DifferentDefinition8{
	struct DifferentDefinition7 member1;
} DifferentDefinition8;

DifferentDefinition8 o8;
