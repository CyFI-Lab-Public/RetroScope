#pragma version(1)
#pragma rs java_package_name(foo)

// expected-error: different number of members
typedef struct DifferentDefinition2{
} DifferentDefinition2;

DifferentDefinition2 o2;
