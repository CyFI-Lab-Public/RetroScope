package foo.bar.testback;

public class Foo {

    private Object mValue;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Foo other = (Foo) obj;
        if (mValue == null) {
            if (other.mValue != null)
                return false;
        } else if (!mValue.equals(other.mValue))
            return false;
        return true;
    }
    
    
    private class Bar {
        protected void doSomething() {
            
        }
    }

    private class Baz extends Bar {
        @Override
        protected void doSomething() {
            
        }
    }
}
