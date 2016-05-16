#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _set_object_allocation_input {
    rs_allocation allocation;
} set_object_allocation_input;

void set_object_allocation(const set_object_allocation_input *in, int *out)
{
    rs_allocation dst;
    rsSetObject(&dst,in->allocation);
    *out = ( dst.p == in->allocation.p ? 1 : 0 );
}

typedef struct _set_object_element_input {
    rs_element element;
} set_object_element_input;

void set_object_element(const set_object_element_input *in, int *out)
{
    rs_element dst;
    rsSetObject(&dst,in->element);
    *out = ( dst.p == in->element.p ? 1 : 0 );
}

typedef struct _set_object_sampler_input {
    rs_sampler sampler;
} set_object_sampler_input;

void set_object_sampler(const set_object_sampler_input *in, int *out)
{
    rs_sampler dst;
    rsSetObject(&dst,in->sampler);
    *out = ( dst.p == in->sampler.p ? 1 : 0 );
}

typedef struct _set_object_script_input {
    rs_script script;
} set_object_script_input;

void set_object_script(const set_object_script_input *in, int *out)
{
    rs_script dst;
    rsSetObject(&dst,in->script);
    *out = ( dst.p == in->script.p ? 1 : 0 );
}

typedef struct _set_object_type_input {
    rs_type type;
} set_object_type_input;

void set_object_type(const set_object_type_input *in, int *out)
{
    rs_type dst;
    rsSetObject(&dst,in->type);
    *out = ( dst.p == in->type.p ? 1 : 0 );
}
