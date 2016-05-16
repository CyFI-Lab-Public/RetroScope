#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: different typedef
typedef float Type1;
typedef struct DifferentDefinition7{
	Type1 member1;
} DifferentDefinition7;

DifferentDefinition7 o7;
