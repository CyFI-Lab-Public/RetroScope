#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

int ID;
int data;

void callBack1Params() {
    rsSendToClientBlocking(ID);
}

void callBack3Params() {
    rsSendToClientBlocking(ID, &data, sizeof(data));
}
