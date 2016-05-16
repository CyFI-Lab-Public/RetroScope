#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: different order(POD)
typedef struct DifferentDefinition5{
	float member2;
	int member1;
} DifferentDefinition5;

DifferentDefinition5 o5;
