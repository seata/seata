package io.seata.common.testmodel;

public class NotExistsNoArgsConstructorTestClass extends TestSuperClass {

    private String f1;

    NotExistsNoArgsConstructorTestClass(String f1) {
        this.f1 = f1;
    }

    public String getF1() {
        return f1;
    }

    public void setF1(String f1) {
        this.f1 = f1;
    }
}
