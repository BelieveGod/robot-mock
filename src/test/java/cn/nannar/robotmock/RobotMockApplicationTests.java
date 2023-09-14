package cn.nannar.robotmock;

import cn.hutool.core.collection.CollUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

@SpringBootTest
class RobotMockApplicationTests {

    @Test
    void contextLoads() {
    }


    public static void main(String[] args) {
        try {

            A a = new A();
            B b = new B();
            List<Object> objectList = new LinkedList<>();
            objectList.add(a);
            objectList.add(b);

            for (Object o : objectList) {
                Method getList = o.getClass().getMethod("getList");
                List<Integer> list=((List<Integer>) getList.invoke(o));
                System.out.println("list = " + list);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static class A{

        public List<Integer> getList(){
            return CollUtil.newArrayList(1, 2, 3);
        }
    }

    public static class B{

        public List<Integer> getList(){
            return CollUtil.newArrayList(4, 2, 3);
        }
    }
}
