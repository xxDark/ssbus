package dev.xdark.ssbus;

interface Dispatcher<E> {

  void dispatch(E event);
}
