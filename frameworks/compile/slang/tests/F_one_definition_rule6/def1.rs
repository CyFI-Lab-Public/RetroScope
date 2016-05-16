#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: mix
typedef struct DifferentDefinition6{
	int member1;
	float member2;
	float member3;
} DifferentDefinition6;

DifferentDefinition6 o6;
