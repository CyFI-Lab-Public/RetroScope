#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation allocation;

void clear_allocation(int *out)
{
    rsClearObject( &allocation );
    *out = ( NULL == allocation.p ? 1 : 0 );
}

rs_element element;

void clear_element(int *out)
{
    rsClearObject( &element );
    *out = ( NULL == element.p ? 1 : 0 );
}

rs_sampler sampler;

void clear_sampler(int *out)
{
    rsClearObject( &sampler );
    *out = ( NULL == sampler.p ? 1 : 0 );
}

rs_script script;

void clear_script(int *out)
{
    rsClearObject( &script );
    *out = ( NULL == script.p ? 1 : 0 );
}

rs_type type;

void clear_type(int *out)
{
    rsClearObject( &type );
    *out = ( NULL == type.p ? 1 : 0 );
}
