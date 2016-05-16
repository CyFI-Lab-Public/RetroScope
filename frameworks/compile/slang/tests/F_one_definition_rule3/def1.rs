#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: different type
typedef struct DifferentDefinition3{
	int member1;
	float member2;
} DifferentDefinition3;

DifferentDefinition3 o3;
