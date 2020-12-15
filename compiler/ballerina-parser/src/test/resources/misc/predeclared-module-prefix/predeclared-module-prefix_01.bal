// Override lang.int with custom import
import ballerina/foo.bar.baz as int;

// Predeclared prefix in annotation and listener decl
@int:annot
listener stream:Element myListner = getListner();

type myRecord record {
    int a;
    *table:tableRecod;
};

type myObject object {
    function foo(int a, any b);
    *object:customObj;
};

public function foo() returns int:c {
    // Using module prefix with predeclared prefix
    int a = int:sum(1, 2, 3);
    int b = int:b;
    // Predeclared prefix in new expression
    Person p = new int:myClass();
    // Predeclared prefix in object constructor expression
    object {int a;} myObj = object object:myObjType {int a = 5;};
    // Predeclared prefix int error constructor expression
    error e = error transaction:transactionError(msg);
    // Predeclared prefix in error type reference in error match pattern
    match (a) {
        error object:customObjectError() => {
            io:println("custom object error");
        }
    }
}
