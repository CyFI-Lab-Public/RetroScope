#include "rs_core.rsh"
#include "rs_graphics.rsh"
#include "rs_structs.h"

/**
* Mesh
*/
extern uint32_t __attribute__((overloadable))
        rsgMeshGetVertexAllocationCount(rs_mesh m) {
    Mesh_t *mesh = (Mesh_t *)m.p;
    if (mesh == NULL) {
        return 0;
    }
    return mesh->mHal.state.vertexBuffersCount;
}

extern uint32_t __attribute__((overloadable))
        rsgMeshGetPrimitiveCount(rs_mesh m) {
    Mesh_t *mesh = (Mesh_t *)m.p;
    if (mesh == NULL) {
        return 0;
    }
    return mesh->mHal.state.primitivesCount;
}

extern rs_allocation __attribute__((overloadable))
        rsgMeshGetVertexAllocation(rs_mesh m, uint32_t index) {
    Mesh_t *mesh = (Mesh_t *)m.p;
    if (mesh == NULL || index >= mesh->mHal.state.vertexBuffersCount) {
        rs_allocation nullAlloc = {0};
        return nullAlloc;
    }
    rs_allocation returnAlloc = {mesh->mHal.state.vertexBuffers[index]};
    return returnAlloc;
}

extern rs_allocation __attribute__((overloadable))
        rsgMeshGetIndexAllocation(rs_mesh m, uint32_t index) {
    Mesh_t *mesh = (Mesh_t *)m.p;
    if (mesh == NULL || index >= mesh->mHal.state.primitivesCount) {
        rs_allocation nullAlloc = {0};
        return nullAlloc;
    }
    rs_allocation returnAlloc = {mesh->mHal.state.indexBuffers[index]};
    return returnAlloc;
}

extern rs_primitive __attribute__((overloadable))
        rsgMeshGetPrimitive(rs_mesh m, uint32_t index) {
    Mesh_t *mesh = (Mesh_t *)m.p;
    if (mesh == NULL || index >= mesh->mHal.state.primitivesCount) {
        return RS_PRIMITIVE_INVALID;
    }
    return mesh->mHal.state.primitives[index];
}
