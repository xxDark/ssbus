package dev.xdark.ssbus;

import org.objectweb.asm.ClassVisitor;

import java.util.LinkedList;
import java.util.List;

final class ClassInfo {

  final List<String> interfaces = new LinkedList<>();
  final ClassVisitor cv;
  final int version;
  final String name;
  String superName;
  int access;

  ClassInfo(ClassVisitor cv, int version, int access, String name, String superName) {
    this.cv = cv;
    this.version = version;
    this.access = access;
    this.name = name;
    this.superName = superName;
  }
}
