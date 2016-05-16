#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: different number of members
typedef struct DifferentDefinition1{
	int member1;
	float member2;
} DifferentDefinition1;

DifferentDefinition1 o1;
