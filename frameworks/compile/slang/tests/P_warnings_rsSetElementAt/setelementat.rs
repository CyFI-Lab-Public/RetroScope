#pragma version(1)
#pragma rs java_package_name(foo)

rs_allocation A;
static void foo() {
    // Basic scalar and floating point types.
    float a = 4.0f;
    double d = 4.0f;
    float2 a2 = {4.0f, 4.0f};
    float3 a3 = {4.0f, 4.0f, 4.0f};
    float4 a4 = {4.0f, 4.0f, 4.0f, 4.0f};
    char c = 4;
    uchar uc = 4;
    short s = 4;
    ushort us = 4;
    int i = 4;
    uint ui = 4;
    long l = 4;
    ulong ul = 4;

    rsSetElementAt(A, &a, 0, 0);
    rsSetElementAt(A, &d, 0, 0);
    rsSetElementAt(A, &a2, 0, 0);
    rsSetElementAt(A, &a3, 0, 0);
    rsSetElementAt(A, &a4, 0, 0);
    rsSetElementAt(A, &c, 0, 0);
    rsSetElementAt(A, &uc, 0, 0);
    rsSetElementAt(A, &s, 0, 0);
    rsSetElementAt(A, &us, 0, 0);
    rsSetElementAt(A, &i, 0, 0);
    rsSetElementAt(A, &ui, 0, 0);
    rsSetElementAt(A, &l, 0, 0);
    rsSetElementAt(A, &ul, 0, 0);

    // No warnings for complex data types
    struct {
        int A;
        int B;
    } P;
    rsSetElementAt(A, &P, 0, 0);

    // No warning for 'long long'
    long long LL = 4.0f;
    rsSetElementAt(A, &LL, 0, 0);

    // Unsupported vector width
    typedef int int5 __attribute__((ext_vector_type(5)));
    int5 i5 = {5, 5, 5, 5, 5};

    rsSetElementAt(A, &i5, 0, 0);
}

