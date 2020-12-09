### 模块

#### nacos-address
address模块: 主要查询nacos集群中节点个数以及IP的列表.

#### nacos-api
api模块: 主要给客户端调用的api接口的抽象.

#### nacos-auth


#### nacos-client
client模块: 主要是对依赖api模块和common模块,对api的接口的实现,给nacos的客户端使用.

#### nacos-cmdb
cmdb模块: 主要是操作的数据的存储在内存中,该模块提供一个查询数据标签的接口.

#### nacos-common
common模块: 主要是通用的工具包和字符串常量的定义

| - common<br>
&emsp;&emsp;| - codec<br>
&emsp;&emsp;&emsp;&emsp;| - Base64 Base64的工具类<br>
&emsp;&emsp;| - constant<br>
&emsp;&emsp;&emsp;&emsp;| - HttpHeaderConsts <br>
&emsp;&emsp;&emsp;&emsp;| - ResponseHandlerType <br>
&emsp;&emsp;| - executor<br>
&emsp;&emsp;&emsp;&emsp;| - ExecutorFactory <br>
&emsp;&emsp;&emsp;&emsp;| - NameThreadFactory 自定义线程工厂<br>
&emsp;&emsp;&emsp;&emsp;| - ThreadPoolManager <br>



#### nacos-config
config模块: 主要是服务配置的管理, 提供api给客户端拉去配置信息,以及提供更新配置的,客户端通过长轮询的更新配置信息.数据存储是mysql.

#### nacos-consistency


#### nacos-console
console模块: 主要是实现控制台的功能.具有权限校验、服务状态、健康检查等功能.

#### nacos-core
core模块: 主要是实现Spring的PropertySource的后置处理器,用于加载nacos的default的配置信息.

#### nacos-distribution
distribution模块: 主要是打包nacos-server的操作,使用maven-assembly-plugin进行自定义打包

#### nacos-example


#### nacos-istio


#### nacos-naming
naming模块: 主要是作为服务注册中心的实现模块,具备服务的注册和服务发现的功能.

#### nacos-sys


#### nacos-test
