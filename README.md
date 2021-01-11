# mybatis-plugin
mybatis插件用于自动Set属性，可扩展性强。新增需要自动设置的属性只需要实现setter类，并指定相应的Code。支持批量插入和duplicate key update。
进行了一定的性能优化，缓存了每个类需要设置的属性，避免每次给对象赋值都需要大量反射操作带来的性能开销。
