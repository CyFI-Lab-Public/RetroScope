import os, bpy, struct
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty

# This is a custom Blender export script to output an object to a format that's easy to use in
# android. While open gl allows the use of an index buffer for vertices, the same cannot be done
# for texture coordinates, which is why this script duplicates the vertices and normals. This
# gives a larger but faster loading file, hence the tongue-in-cheek name "Compressed" object file.
# The format is number of vertices + list of vertices (3 coord, 3 normal, 2 texcoord)
bl_info = {
        "name": "COB Exporter",
        "description": "Exports the active scene into a Compressed Object file.",
        "author": "Stuart Scott",
        "version": (1, 0, 0),
        "blender": (2, 6, 2),
        "api": 36339,
        "location": "File > Export > COB Exporter (.cob)",
        "warning": "", # used for warning icon and text in addons panel
        "wiki_url": "",
        "tracker_url": "",
        "category": "Import-Export"
        }

class COBExporter(bpy.types.Operator, ExportHelper):
    '''Exports the current scene into a Compressed Object format.'''
    bl_idname = "export.cob_exporter"  # this is important since its how bpy.ops.export.cob_exporter is constructed
    bl_label = "COB Exporter"

    filename_ext = ".cob"

    filter_glob = StringProperty(default="*.cob", options={'HIDDEN'})

    def execute(self, context):
        mesh = context.active_object
        if mesh.type != 'MESH':
            print("Active object is not a mesh")
            return {'FINISHED'}
        if mesh.mode != 'OBJECT':
            bpy.ops.object.mode_set(mode='OBJECT', toggle=False)
        print("Writing "+mesh.name+" to "+self.filepath)
        uvtex = mesh.data.uv_textures.active # points to active texture
        f = open(self.filepath, 'wb')
        f.write(struct.pack(">i", len(uvtex.data) * 3))# write length
        for uv_index, uv_itself in enumerate(uvtex.data):
            # get uv for this face
            uvs = uv_itself.uv1, uv_itself.uv2, uv_itself.uv3
            for vertex_index, vertex_itself in enumerate(mesh.data.faces[uv_index].vertices):
                # for each vertex in the face
                vertex = mesh.data.vertices[vertex_itself]
                v = vertex.co.xyz
                n = vertex.normal.xyz
                t = uvs[vertex_index]
                f.write(struct.pack(">ffffffff", v[0], v[1], v[2], n[0], n[1], n[2], t[0], 1 - t[1]))
        f.close()
        return {'FINISHED'}

    @classmethod
    def poll(cls, context):
        return context.active_object != None

# Only needed if you want to add into a dynamic menu
def menu_func(self, context):
    default_path = os.path.splitext(bpy.data.filepath)[0] + ".cob"
    self.layout.operator(COBExporter.bl_idname, text="Compressed Object (.cob)").filepath = default_path

def register():
    bpy.utils.register_module(__name__)
    bpy.types.INFO_MT_file_export.append(menu_func)

def unregister():
    bpy.utils.unregister_module(__name__)
    bpy.types.INFO_MT_file_export.remove(menu_func)

if __name__ == "__main__":
    register()
