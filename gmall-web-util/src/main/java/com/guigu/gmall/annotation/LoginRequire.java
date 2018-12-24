package com.guigu.gmall.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)  //　@Target说明了Annotation所修饰的对象范围：
@Retention(RetentionPolicy.RUNTIME)  // 自定义运行时注解
public @interface LoginRequire {   // 自定义注解，该注解作用于方法上，并且是个运行时注解
    boolean  needSuccess() default true; //该方法默认返回true
}

/* 取值(ElementType)有：
　　　　1.CONSTRUCTOR:用于描述构造器
　　　　2.FIELD:用于描述域
　　　　3.LOCAL_VARIABLE:用于描述局部变量
　　　　4.METHOD:用于描述方法
　　　　5.PACKAGE:用于描述包
　　　　6.PARAMETER:用于描述参数
　　　　7.TYPE:用于描述类、接口(包括注解类型) 或enum声明

注解分为三类：
1、RetentionPolicy.SOURCE：注解只保留在源文件，当Java文件编译成class文件的时候，注解被遗弃；
2、RetentionPolicy.CLASS：注解被保留到class文件，但jvm加载class文件时候被遗弃，这是默认的生命周期；
3、RetentionPolicy.RUNTIME：注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在；
   这3个生命周期分别对应于：Java源文件(.java文件) ---> .class文件 ---> 内存中的字节码    (生命周期长度 SOURCE < CLASS < RUNTIME)


通过反射来获取运行时注解




*/