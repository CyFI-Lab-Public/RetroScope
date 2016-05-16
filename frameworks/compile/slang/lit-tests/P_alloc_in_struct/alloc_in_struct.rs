// RUN: %Slang %s
// RUN: %rs-filecheck-wrapper %s
// CHECK: define void @.rs.dtor()

#pragma version(1)
#pragma rs java_package_name(alloc_in_struct)

struct s {
    rs_allocation a;
} myStruct;

