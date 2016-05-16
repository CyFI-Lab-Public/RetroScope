#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: different name
typedef struct DifferentDefinition4{
	int member1;
	float member3;
} DifferentDefinition4;

DifferentDefinition4 o4;
