### 热更新模块
本模块为热更新模块，实现类文件和普通文件和表格热更新。

#### 资源目录
要求有一个统一的res目录，需要通过启动参数指定res目录。
我们所有的表格，要更新的class，以及其它文件必须在res目录下，这样方便统一管理。
其中： 
+ class文件在单独的子目录classes目录下，必须按照包级结构存放，这样才能找到对应的class文件，这需要自动化。
+ excel文件在单独的子目录config下，可以平铺也可以嵌套。
+ 其它文件可以在单独的子目录，也可以直接在res文件夹下。

#### 必须根据http请求更新
Q: 为什么不是检测到变化就直接更新，还需要监听http请求才更新？是否有点笨拙?  
A: 自动更新最大的问题：**不受控制**。  
这可能会导致错误的更新！比如改了个配置，刚提交到远程服务器，这时候策划说他还要改一下。。。  
另外，自动更新无法保证原子性，无法保证所有的文件同时更新。额外的好处是，省了很多不必要的开销。

#### 必须进行本地热更新测试
1. 必须先在镜像服进行热更新测试。
2. 必须现在本地启服测试。

#### 热更新逻辑必须幂等
我们要求所有的回调逻辑（热加载）逻辑必须是幂等的，换个说法：只要文件相同，不论回调逻辑执行多少次，都必须保证相同的结果，且不影响业务！  
为了测试实现的正确性，我们在运行期间（测试期间）会不定期的执行回调逻辑，如果有缺陷，则会在测试期间暴露出来。

#### 更新失败处理
我们强制要求了所有的热更回调等逻辑必须是**幂等**的，因此只要能再次调用更新即可，这有两种方式：
1. 再次修改文件，使文件的MD5改变，然后再次执行全部更新逻辑。
2. 指定文件强制更新，这不会检测MD5，无论文件是否改变，都会执行对应的更新逻辑。

#### 基于Excel文件的热更新，而非基于Sheet的更新
我们的设计基于以下假设:  
1. 我们都喜欢使用多sheet的方式，而不是一个excel只有单sheet的形式。
2. 当一个excel的md5改变时，越大的sheet发生改变的可能性越大。

基于假设1，我们必须支持多sheet的形式，此时我们使用sheet的名字确定其唯一性，而不是excel的名字。  
基于假设2，当一个excel的md5改变时，我们直接更新该文件内的所有sheet，从而避免比较每个sheet的相等性（比较sheet是否发生改变的开销较大）。

#### 配表原则
1. 我们还是尽量将相关的sheet放在同一个excel，只有当表格较大时，才考虑优化。
2. 如果一个sheet较大，且同表的其它sheet与其依赖较小，则拆分出来单独成表，比如物品表。
3. 如果一个excel中存在多个大型的sheet，考虑适当的拆表。

#### 性能瓶颈
性能瓶颈在解析Excel部分，即将Excel转换为{@link Sheet}的过程。
Excel底层存储结构是XML，解析相当慢，占用CPU资源较多，因此属于计算密集型任务，分配的线程数不必太多。

* 环境:
cpu: i5-4570（4核）  
机器内存: 16G  
JDK: Amazon Corretto - jdk11.0.7_10  
启动参数: -server -ea -Xms2048M -Xmx2048m
表格: 文件大小927k，3870行，49列，服务器读取列数35列。  

* 读取单个文件：  
Excel -> Sheet: 3200ms ~ 3800ms  
Sheet -> 自定义结构: 80ms ~ 90ms  

* 单线程读取500个文件:  
Executor: DirectExecutor  
加载文件耗时：约360s~370s

* 并发读取500个文件:  
线程数：2  
Executor: ThreadPoolExecutor  
加载文件耗时：约190s~200s

* 并发读取500个文件:  
线程数：4
Executor: ThreadPoolExecutor  
加载文件耗时：约120s~130s


    public static void main(String[] args) throws Exception {
        final StepWatch stepWatch = StepWatch.createStarted("PerformanceTest:main");
        final int poolSize = Runtime.getRuntime().availableProcessors();
        final int fileNum = 500;
        try {
            copyFiles(fileNum);
            stepWatch.logStep("copyFiles");

            final TemplateMrg templateMrg = new TemplateMrg();
            final TimerSystem timerSystem = new DefaultTimerSystem(TimeProviders.realtimeProvider());
            final FileReloadMgr fileReloadMgr = new FileReloadMgr(PROJECT_RES_DIR, templateMrg, timerSystem, newThreadPool(poolSize),
                    30 * TimeUtils.SEC, 5 * TimeUtils.SEC, 2 * TimeUtils.MIN);
            final ExcelReloadMgr excelReloadMgr = new ExcelReloadMgr(PROJECT_RES_DIR, CONFIG_DIR_NAME, fileReloadMgr, templateMrg, DefaultCellValueParser::new);

            final List<GoodsReader> goodsReaderList = new ArrayList<>(fileNum);
            for (int index = 0; index < fileNum; index++) {
                goodsReaderList.add(new GoodsReader(index));
            }
            excelReloadMgr.registerReaders(goodsReaderList);
            stepWatch.logStep("registerReaders");

            // 准备就绪后加载文件(excel->sheet->自定义结构)
            fileReloadMgr.loadAll();
            stepWatch.logStep("loadFiles");

            // 加载表格
            excelReloadMgr.loadAll();
            stepWatch.logStep("loadSheets");
        } finally {
            deleteCopiedFiles(fileNum);
            // 输出详细信息
            System.out.println(stepWatch);
        }
    }   

部分日志：  
单线程

[2020-12-03 15:59:40,718][INFO,FileReloadMgr] loadAll started
[2020-12-03 16:05:49,107][INFO,FileReloadTask] run completed, stepInfo StepWatch[FileReloadTask=368372ms][statisticFileStats=2423ms,readChangedFiles=365947ms]
[2020-12-03 16:05:49,109][INFO,FileReloadMgr] StepWatch[FileReloadMrg:reloadImpl=2ms][buildSandbox=1ms,validateOther=1ms]
[2020-12-03 16:05:49,109][INFO,FileReloadMgr] loadAll completed, fileNum 500, stepInfo StepWatch[FileReloadMgr:loadAll=368387ms][join=368384ms,reloadImpl=3ms]
[2020-12-03 16:05:49,111][INFO,ExcelReloadMgr] StepWatch[ExcelReloadMrg:reloadImpl=1ms][buildSandbox=1ms,validateOther=0ms]
StepWatch[PerformanceTest:main=371634ms][copyFiles=2042ms,registerReaders=480ms,loadFiles=368391ms,loadSheets=2ms]

[2020-12-03 16:12:50,948][INFO,FileReloadMgr] loadAll started
[2020-12-03 16:18:53,463][INFO,FileReloadTask] run completed, stepInfo StepWatch[FileReloadTask=362498ms][statisticFileStats=2357ms,readChangedFiles=360139ms]
[2020-12-03 16:18:53,465][INFO,FileReloadMgr] StepWatch[FileReloadMrg:reloadImpl=2ms][buildSandbox=1ms,validateOther=0ms]
[2020-12-03 16:18:53,465][INFO,FileReloadMgr] loadAll completed, fileNum 500, stepInfo StepWatch[FileReloadMgr:loadAll=362514ms][join=362511ms,reloadImpl=3ms]
[2020-12-03 16:18:53,467][INFO,ExcelReloadMgr] StepWatch[ExcelReloadMrg:reloadImpl=1ms][buildSandbox=1ms,validateOther=0ms]
StepWatch[PerformanceTest:main=365592ms][copyFiles=1921ms,registerReaders=428ms,loadFiles=362517ms,loadSheets=2ms]

2线程

[2020-12-03 16:19:16,545][INFO,FileReloadMgr] loadAll started
[2020-12-03 16:22:30,432][INFO,FileReloadTask] run completed, stepInfo StepWatch[FileReloadTask=193870ms][statisticFileStats=1182ms,readChangedFiles=192686ms]
[2020-12-03 16:22:30,434][INFO,FileReloadMgr] StepWatch[FileReloadMrg:reloadImpl=2ms][buildSandbox=1ms,validateOther=0ms]
[2020-12-03 16:22:30,434][INFO,FileReloadMgr] loadAll completed, fileNum 500, stepInfo StepWatch[FileReloadMgr:loadAll=193887ms][join=193884ms,reloadImpl=3ms]
[2020-12-03 16:22:30,436][INFO,ExcelReloadMgr] StepWatch[ExcelReloadMrg:reloadImpl=1ms][buildSandbox=1ms,validateOther=0ms]
StepWatch[PerformanceTest:main=197003ms][copyFiles=1948ms,registerReaders=428ms,loadFiles=193891ms,loadSheets=3ms]

[2020-12-03 16:24:13,466][INFO,FileReloadMgr] loadAll started
[2020-12-03 16:27:29,628][INFO,FileReloadTask] run completed, stepInfo StepWatch[FileReloadTask=196146ms][statisticFileStats=1173ms,readChangedFiles=194972ms]
[2020-12-03 16:27:29,629][INFO,FileReloadMgr] StepWatch[FileReloadMrg:reloadImpl=3ms][buildSandbox=1ms,validateOther=0ms]
[2020-12-03 16:27:29,629][INFO,FileReloadMgr] loadAll completed, fileNum 500, stepInfo StepWatch[FileReloadMgr:loadAll=196161ms][join=196158ms,reloadImpl=3ms]
[2020-12-03 16:27:29,631][INFO,ExcelReloadMgr] StepWatch[ExcelReloadMrg:reloadImpl=1ms][buildSandbox=1ms,validateOther=0ms]
StepWatch[PerformanceTest:main=199315ms][copyFiles=2002ms,registerReaders=420ms,loadFiles=196164ms,loadSheets=2ms]

4线程

[2020-12-03 15:55:15,922][INFO,FileReloadTask] run completed, stepInfo StepWatch[FileReloadTask=128338ms][statisticFileStats=723ms,readChangedFiles=127614ms]
[2020-12-03 15:55:15,923][INFO,FileReloadMgr] StepWatch[FileReloadMrg:reloadImpl=1ms][buildSandbox=1ms,validateOther=0ms]
[2020-12-03 15:55:15,923][INFO,FileReloadMgr] loadAll completed, fileNum 500, stepInfo StepWatch[FileReloadMgr:loadAll=128361ms][join=128359ms,reloadImpl=1ms]
[2020-12-03 15:55:15,925][INFO,ExcelReloadMgr] StepWatch[ExcelReloadMrg:reloadImpl=1ms][buildSandbox=1ms,validateOther=0ms]
StepWatch[PerformanceTest:main=131534ms][copyFiles=1962ms,registerReaders=442ms,loadFiles=128364ms,loadSheets=2ms]

[2020-12-03 15:56:33,969][INFO,FileReloadMgr] loadAll started
[2020-12-03 15:58:36,651][INFO,FileReloadTask] run completed, stepInfo StepWatch[FileReloadTask=122655ms][statisticFileStats=898ms,readChangedFiles=121756ms]
[2020-12-03 15:58:36,651][INFO,FileReloadMgr] StepWatch[FileReloadMrg:reloadImpl=1ms][buildSandbox=0ms,validateOther=1ms]
[2020-12-03 15:58:36,651][INFO,FileReloadMgr] loadAll completed, fileNum 500, stepInfo StepWatch[FileReloadMgr:loadAll=122681ms][join=122679ms,reloadImpl=2ms]
[2020-12-03 15:58:36,653][INFO,ExcelReloadMgr] StepWatch[ExcelReloadMrg:reloadImpl=1ms][buildSandbox=1ms,validateOther=0ms]
StepWatch[PerformanceTest:main=126065ms][copyFiles=2233ms,registerReaders=441ms,loadFiles=122685ms,loadSheets=2ms]