#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void callback(int id){
    rsSendToClient(id);
}