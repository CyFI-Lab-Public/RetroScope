#pragma version(1)
#pragma rs java_package_name(foo)

const int ic = 99;

int ica[2] = {ic, 1000};

float fa[4] = {1.0, 9.9999f};
double da[2] = {7.0, 8.88888};
char ca[4] = {'a', 7, 'b', 'c'};
short sa[4] = {1, 1, 2, 3};
int ia[4] = {5, 8};
long la[2] = {13, 21};
long long lla[4] = {34};
bool ba[3] = {true, false};

// Clang should implicitly promote this type to have a constant size of 3.
char implicitArray[] = { 'a', 'b', 'c' };

// Clang should implicitly promote this type to have a constant size of 1.
// Note that this creates a warning.
char implicitArrayUninit[];
