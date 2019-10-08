### TCC


#### TCC 简介

在2PC（两阶段提交）协议中，事务管理器分两阶段协调资源管理，资源管理器对外提供了3个操作，分别是一阶段的准备操作，二阶段的提交操作和回滚操作；

TCC服务作为一种事务资源，遵循两阶段提交协议，由业务层面自定义，需要用户根据业务逻辑编码实现；其包含Try、Confirm 和 Cancel 3个操作，其中Try操作对应分布式事务一阶段的准备，Confirm操作对应分布式事务二阶段提交，Cancel对应分布式事务二阶段回滚：

- Try：

资源的检查和预留；

- Comfirm：

使用预留的资源，完成真正的业务操作；要求Try成功Confirm 一定要能成功；

- Cancel：

释放预留资源；


TCC的3个方法均由用户根据业务场景编码实现，并对外发布成微服务，供事务管理器调用；事务管理器在一阶段调用TCC的Try方法，在二阶段提交时调用Confirm方法，在二阶段回滚时调用Cancel方法。


#### TCC实现

##### 1、TCC微服务

TCC服务由用户编码实现并对外发布成微服务，目前支持3种形式的TCC微服务，分别是：

- SofaRpc 服务

用户将实现的TCC操作对外发布成 SofaRpc 服务，事务管理器通过订阅SofaRpc服务，来协调TCC资源；

- Dubbo 服务

将TCC发布成dubbo服务，事务管理器订阅dubbo服务，来协调TCC资源；

- Local TCC

本地普通的TCC Bean，非远程服务；事务管理器通过本地方法调用，来协调TCC 资源；

##### 2、TCC资源动态代理

对TCC服务进行动态代理，GlobalTransactionScanner中当扫描到TCC 服务的 ‘reference’时，会对其进行动态代理；

TCC 动态代理的主要功能是：生成TCC运行时上下文、透传业务参数、注册分支事务记录；

#### 模块说明

seata-tcc 包含TCC主要代码：

- interceptor：TCC 动态代理；

- remoting：TCC微服务扫描、RPC协议解析、TCC资源注册；

- TccResourceManager、RMHandlerTCC： TCC资源管理器；




#### 其他说明：

本此为了区分AT、TCC 2种资源类型，在客户端和服务端通信的接口中，均添加了 BranchType 参数；




