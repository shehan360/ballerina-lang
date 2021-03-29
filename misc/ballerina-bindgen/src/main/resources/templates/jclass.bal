import ballerina/jballerina.java;

# Ballerina class mapping for the Java `FULL_CLASS_NAME` CLASS_TYPE.
@java:Binding {
    'class: "FULL_CLASS_NAME"
}
ACCESS_MODIFIERdistinct class SIMPLE_CLASS_NAME {

    *java:JObject;

    # The `handle` field that stores the reference to the `FULL_CLASS_NAME` object.
    ACCESS_MODIFIERhandle jObj;

    # The init function of the Ballerina class mapping the `FULL_CLASS_NAME` Java CLASS_TYPE.
    #
    # + obj - The `handle` value containing the Java reference of the object.
    ACCESS_MODIFIERfunction init(handle obj) {
        self.jObj = obj;
    }

    # The function to retrieve the string representation of the Ballerina class mapping the `FULL_CLASS_NAME` Java CLASS_TYPE.
    #
    # + return - The `string` form of the Java object instance.
    ACCESS_MODIFIERfunction toString() returns string {
        return java:toString(self.jObj) ?: "null";
    }
}
