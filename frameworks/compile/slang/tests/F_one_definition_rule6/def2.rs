#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: mix
typedef struct DifferentDefinition6{
	float member5;
	int member2;
} DifferentDefinition6;

DifferentDefinition6 o6;
