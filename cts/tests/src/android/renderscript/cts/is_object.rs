#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct _object_allocation_input {
    rs_allocation allocation;
} object_allocation_input;

void is_object_allocation( const object_allocation_input *in, int *out)
{
    *out = rsIsObject(in->allocation)==false ? 0 : 1;
}

typedef struct _object_element_input {
    rs_element element;
} object_element_input;

void is_object_element( const object_element_input *in, int *out)
{
    *out = rsIsObject(in->element)==false ? 0 : 1;
}

typedef struct _object_sampler_input {
    rs_sampler sampler;
} object_sampler_input;

void is_object_sampler( const object_sampler_input *in, int *out)
{
    *out = rsIsObject(in->sampler)==false ? 0 : 1;
}

typedef struct _object_script_input {
    rs_script script;
} object_script_input;

void is_object_script( const object_script_input *in, int *out)
{
    *out = rsIsObject(in->script)==false ? 0 : 1;
}

typedef struct _object_type_input {
    rs_type type;
} object_type_input;

void is_object_type( const object_type_input *in, int *out)
{
    *out = rsIsObject(in->type)==false ? 0 : 1;
}
