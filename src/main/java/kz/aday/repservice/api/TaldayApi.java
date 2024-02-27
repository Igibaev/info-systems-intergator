package kz.aday.repservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aday.repservice.talday.TaldayGetterRepository;
import kz.aday.repservice.talday.model.StatCombination;
import kz.aday.repservice.talday.model.StatData;
import kz.aday.repservice.talday.model.StatDataPeriodList;
import kz.aday.repservice.talday.model.StatDataRequest;
import kz.aday.repservice.talday.model.StatFilter;
import kz.aday.repservice.talday.model.StatFilterRequest;
import kz.aday.repservice.talday.model.StatInfo;
import kz.aday.repservice.talday.model.StatMeasure;
import kz.aday.repservice.talday.model.StatPeriod;
import kz.aday.repservice.talday.model.StatSegment;
import kz.aday.repservice.talday.model.Stats;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;
import reactor.util.retry.RetrySpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TaldayApi {
    private static final RetryBackoffSpec RETRY_TIMES = RetrySpec.fixedDelay(100, Duration.ofSeconds(5));

    private static final ParameterizedTypeReference statPeriodParameterized = new ParameterizedTypeReference<List<StatPeriod>>() {
    };
    private static final ParameterizedTypeReference statsParameterized = new ParameterizedTypeReference<Stats>() {
    };
    private static final ParameterizedTypeReference statInfoParameterized = new ParameterizedTypeReference<StatInfo>() {
    };
    private static final ParameterizedTypeReference statSegmentParameterized = new ParameterizedTypeReference<List<StatSegment>>() {
    };
    private static final ParameterizedTypeReference statMeasureParameterized = new ParameterizedTypeReference<List<StatMeasure>>() {
    };
    private static final ParameterizedTypeReference statFilterParameterized = new ParameterizedTypeReference<List<StatFilter>>() {
    };
    private static final ParameterizedTypeReference statDataParameterized = new ParameterizedTypeReference<List<StatData>>() {
    };
    private static final ParameterizedTypeReference statDataPeriodListParameterized = new ParameterizedTypeReference<StatDataPeriodList>() {
    };
    private static final ParameterizedTypeReference statCombinationsParameterized = new ParameterizedTypeReference<List<StatCombination>>() {
    };

    private static final String ID = "id";
    private static final String IDX = "idx";
    private static final String NODE = "node";
    private static final String START = "start";
    private static final String PAGE = "page";
    private static final String LIMIT = "limit";
    private static final String PERIODS = "periods";
    private static final String PERIOD_ID = "periodId";
    private static final String P_PERIOD_ID = "p_period_id";
    private static final String INDEX_ID = "indexId"; // идентифкатор показателя
    private static final String P_INDEX_ID = "p_index_id"; // идентифкатор показателя
    private static final String P_MEASURE_ID = "p_measure_id"; // идентифкатор показателя
    private static final String TERMS = "terms";
    private static final String P_TERMS = "p_terms";
    private static final String P_TERM_ID = "p_term_id";
    private static final String P_PARENT_ID = "p_parent_id";
    private static final String P_DIC_IDS = "p_dicIds";
    private static final String ORDER_NUM = "orderNum";
    private static final String INDICATORS = "indicators";
    private static final String IND_COUNT = "indCount";
    private static final String CHECK_ALL = "checkAll";

    public static final String INDEX_IS_OUT_OF_BOUND = "Индекс за пределами диапазона";
    public static final String TITLE_TAG_CLOSE = "</title>";
    public static final String TITLE_TAG_OPEN = "<title>";
    private static final String DIC_IDS = "dics";
    private static final String DIC = "dic";


    private String getStatInfo = "https://taldau.stat.gov.kz/ru/Api/GetIndexAttributes";
    private String getStatsByPeriod = "https://taldau.stat.gov.kz/ru/Search/getSearchPageGridData";
    private String getPeriodsForStat = "https://taldau.stat.gov.kz/ru/Search/GetPeriodNodes";
    private String getSegmentList = "https://taldau.stat.gov.kz/ru/NewIndex/GetSegmentList";
    private String getStatMeasures = "https://taldau.stat.gov.kz/ru/Index/getMeasureTreeNodes";
    private String getFilterListByAndPeriodIdAndStatIdAndDicIdsAndDic = "https://taldau.stat.gov.kz/ru/PivotGrid/getTermBranch";
    private String getStatDataByPeriodIdAndStatIdAndSegmentTermsAndSegmentDicsAndMeasureIdAndParentId =
            "https://taldau.stat.gov.kz/ru/NewIndex/GetIndexTreeData";
    private String getDateListForStatDataByStatId = "https://taldau.stat.gov.kz/ru/NewIndex/GetIndexPeriods";
    private String getStatCombinations = "https://taldau.stat.gov.kz/ru/PivotGrid/getCombinations";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final TaldayGetterRepository taldayGetterRepository;

    public TaldayApi(@Qualifier("taldayApiClient") WebClient webClient, TaldayGetterRepository taldayGetterRepository) {
        this.webClient = webClient;
        this.taldayGetterRepository = taldayGetterRepository;
        this.objectMapper = new ObjectMapper();
    }

    public List<StatFilter> getFiltersByStatCombination(Long statPeriodId, Long statId, String dicIds, String dic) {
        return taldayGetterRepository.getAllStatFiltersByStatCombination(statPeriodId, statId, dicIds, dic);
    }

    public Mono<StatDataPeriodList> getPeriodListByStatDataRequest(StatDataRequest request) {
        log.info("Get StatDataPeriodList by StatDataRequest:{}", request);
        return webClient.post()
                .uri(getDateListForStatDataByStatId)
                .body(
                        BodyInserters
                                .fromFormData(P_INDEX_ID, request.getStatId().toString())
                                .with(P_PERIOD_ID, request.getStatPeriodId().toString())
                                .with(P_MEASURE_ID, request.getStatMeasureId())
                                .with(P_TERM_ID, request.getFirstTermId())
                                .with(P_TERMS, request.getFilterTermIds())
                                .with(P_DIC_IDS, request.getDicIds())
                                .with(IDX, StaticData.IDX)
                                .with(P_PARENT_ID, Optional.ofNullable(request.getParentId()).orElse("null"))
                )
                .retrieve()
                .bodyToMono(statDataPeriodListParameterized)
                .retryWhen(RETRY_TIMES.filter(this::is5xxErrorArrayOutOfBound))
                .onErrorResume((mono) -> Mono.just(new ArrayList()))
                .log();
    }

    public Mono<List<StatData>> getStatDataByStatDataRequest(StatDataRequest request) {
        List<StatData> statDataList = taldayGetterRepository.getAllStatDataByStatDataRequest(request);
        if (!statDataList.isEmpty()) {
            request.setFound(true);
            return Mono.just(statDataList);
        }
        return webClient.post()
                .uri(getStatDataByPeriodIdAndStatIdAndSegmentTermsAndSegmentDicsAndMeasureIdAndParentId)
                .body(
                        BodyInserters
                                .fromFormData(P_INDEX_ID, request.getStatId().toString())
                                .with(P_PERIOD_ID, request.getStatPeriodId().toString())
                                .with(P_MEASURE_ID, request.getStatMeasureId())
                                .with(P_TERM_ID, request.getFirstTermId())
                                .with(P_TERMS, request.getFilterTermIds())
                                .with(P_DIC_IDS, request.getDicIds())
                                .with(IDX, StaticData.IDX)
                                .with(P_PARENT_ID, Optional.ofNullable(request.getParentId()).orElse("null"))
                )
                .retrieve()
                .bodyToMono(statDataParameterized)
                .retry(50);
    }

    public Mono<List<StatFilter>> getFilterListByStatFilterRequest(StatFilterRequest statFilterRequest) {
        return getFilterListByPeriodIdAndStatIdAndDicsIdsAndDicIdAndIdx(
                statFilterRequest.getStatPeriodId(),
                statFilterRequest.getStatId(),
                statFilterRequest.getDicIds(),
                statFilterRequest.getDic(),
                statFilterRequest.getIdx()
        );
    }

    public Mono<List<StatFilter>> getFilterListByPeriodIdAndStatIdAndDicsIdsAndDicIdAndIdx(
            Long statPeriodId,
            Long statId,
            String dicIds,
            String dic,
            Integer idx
    ) {
        List<StatFilter> statFilters = taldayGetterRepository.getAllStatFiltersBy(statPeriodId, statId, dicIds, dic);
        if (!statFilters.isEmpty()) {
            log.info("Found in database by params:[{},{},{},{}]", statPeriodId, statId, dicIds, dic);
            return Mono.just(statFilters);
        } else {
            log.info("Not Found in database by params:[{},{},{},{}]", statPeriodId, statId, dicIds, dic);
        }
        return webClient.post()
                .uri(getFilterListByAndPeriodIdAndStatIdAndDicIdsAndDic)
                .body(BodyInserters
                        .fromFormData(INDEX_ID, statId.toString())
                        .with(PERIOD_ID, statPeriodId.toString())
                        .with(CHECK_ALL, StaticData.checkAll)
                        .with(IDX, idx == null ? "0" : idx.toString())
                        .with(DIC_IDS, dicIds)
                        .with(DIC, dic)
                        .with(NODE, StaticData.node))
                .retrieve()
                .bodyToMono(statFilterParameterized)
                .retryWhen(RETRY_TIMES.filter(this::is5xxErrorArrayOutOfBound))
                .onErrorResume((mono) -> Mono.just(new ArrayList()))
                .log();
    }

    // measureId from StatInfo
    public Mono<List<StatMeasure>> getStatMeasures(String measureId) {
        List<StatMeasure> statMeasures = taldayGetterRepository.getAllMeasuresByMeasureId(measureId);
        if (!statMeasures.isEmpty()) {
            log.info("Found in database by measureId:{}", measureId);
            return Mono.just(statMeasures);
        }
        String url = getStatMeasures + "?node=" + measureId.toString();
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(statMeasureParameterized)
                .retryWhen(RETRY_TIMES.filter(this::is5xxErrorArrayOutOfBound))
                .onErrorResume((mono) -> Mono.just(new ArrayList()))
                .log();
    }

    // получение все той же классификации для пивот таблиц но можно использовать и для другой цели
    public Mono<List<StatCombination>> getStatCombinationsByPeriodIdAndStatId(Long periodId, Long statId) {
        List<StatCombination> statCombinations = taldayGetterRepository.getAllStatCombinationsByPeriodIdAndStatId(periodId, statId);
        if (!statCombinations.isEmpty()) {
            log.info("Found in database by periodId:{} statId:{}", periodId, statId);
            return Mono.just(statCombinations);
        }
        String url = String.format("%s?indicators=%s&periodId=%s&indCount=1&page=1&start=0&limit=250",
                getStatCombinations, statId, periodId);
        return webClient.post()
                .uri(url)
                .retrieve()
                .bodyToMono(statCombinationsParameterized)
                .retryWhen(RETRY_TIMES.filter(this::is5xxErrorArrayOutOfBound))
                .onErrorResume((mono) -> Mono.just(new ArrayList()))
                .log();
    }

    // получение классификации
    public Mono<List<StatSegment>> getStatSegmentsByPeriodIdAndStatId(Long periodId, Long statId) {
        List<StatSegment> statSegments = taldayGetterRepository.getAllStatSegmentsByPeriodIdAndStatId(periodId, statId);
        if (!statSegments.isEmpty()) {
            log.info("Found in database by periodId:{} statId:{}", periodId, statId);
            return Mono.just(statSegments);
        }
        return webClient.post()
                .uri(getSegmentList)
                .body(
                        BodyInserters
                                .fromFormData(INDEX_ID, statId.toString())
                                .with(PERIOD_ID, periodId.toString())
                )
                .retrieve()
                .bodyToMono(statSegmentParameterized)
                .retryWhen(RETRY_TIMES.filter(this::is5xxErrorArrayOutOfBound))
                .onErrorResume((mono) -> Mono.just(new ArrayList()))
                .log();
    }

    public Mono<StatInfo> getStatInfoByStatId(Long statId) {
        StatInfo statInfo = taldayGetterRepository.getStatInfoByStatId(statId);
        if (statInfo != null) {
            log.info("Found in database by statId:{}", statId);
            return Mono.just(statInfo);
        }
        String url = getStatInfo + "?indexId=" + statId.toString();
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(statInfoParameterized)
                .retry()
                .onErrorResume((mono) -> Mono.empty())
                .log();
    }

    public Mono<Stats> getStatsByPeriodId(@NonNull Long periodId) {
        Stats savedStats = taldayGetterRepository.getStatsByPeriod(periodId);
        if (savedStats != null && savedStats.getResults() != null && !savedStats.getResults().isEmpty()) {
            log.info("Found in database by periodId:{}", periodId);
            return Mono.just(savedStats);
        }
        return webClient.post()
                .uri(getStatsByPeriod)
                .body(
                        BodyInserters
                                .fromFormData(START, StaticData.start)
                                .with(LIMIT, StaticData.limit)
                                .with("tree", StaticData.tree)
                                .with(PERIODS, periodId.toString())
                )
                .retrieve()
                .bodyToMono(statsParameterized)
                .retryWhen(RETRY_TIMES)
                .onErrorResume((mono) -> Mono.just(null))
                .log();
    }

    public Mono<List<StatPeriod>> getPeriodsForStat() {
        List<StatPeriod> savedStatPeriods = taldayGetterRepository.getAllStatPeriods();
        if (!savedStatPeriods.isEmpty()) {
            log.info("Found in database all periods");
            return Mono.just(savedStatPeriods);
        }
        return webClient.get()
                .uri(getPeriodsForStat)
                .retrieve()
                .bodyToMono(statPeriodParameterized)
                .retryWhen(RETRY_TIMES)
                .onErrorResume((mono) -> Mono.just(new ArrayList()))
                .log();
    }

    private boolean is5xxErrorArrayOutOfBound(Throwable throwable) {
        throwable.printStackTrace();
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException clientResponseException = (WebClientResponseException) throwable;
            log.error("Caught error:{}", throwable.getMessage());
            if (clientResponseException.getStatusCode().is5xxServerError()) {
                String responseBody = clientResponseException.getResponseBodyAsString();
                if (responseBody.contains(TITLE_TAG_CLOSE) && responseBody.contains(TITLE_TAG_OPEN)) {
                    log.error("Message: status:{} title:{}",
                            clientResponseException.getStatusText(),
                            responseBody.substring(
                                    responseBody.indexOf(TITLE_TAG_OPEN),
                                    responseBody.indexOf(TITLE_TAG_CLOSE)
                            )
                    );
                }
                if (responseBody.contains(INDEX_IS_OUT_OF_BOUND)) {
                    return true;
                }
                return false;
            } else {
                return false;
            }

        } else {
            return false;
        }
    }

    private interface StaticData {
        String tree = "-1,700894,700895,700896,700899,700903,2709376,700908,700911,700912,4023005,700914,700916,700925,700928,700931,18198759,700934,2709379,2709380,4023003,700937,700938,700940,700947,700948,700951,700956,700957,18198797,700958,18198806,700961,700964,700965,700967,700973,700974,700975,700977,2709438,700984,700985,700986,700988,2709439,700996,700997,701003,701012,701013,701014,701020,701028,701034,701036,3769031,3769066,701051,701052,701060,701062,701063,701064,701070,701075,701076,4761786,4761796,4761800,4761853,4762775,4772340,4772380,4794865,4794869,4794873,4794927,701081,701082,701083,701084,701087,701088,701093,701098,701099,701100,701101,701108,701109,701112,701113,701116,701117,701118,701120,701121,3769071,701124,701125,2972977,2972981,701128,701129,701130,701133,701136,701137,701138,701140,701143,701146,701150,701153,77208810,77208811,4760741,4760742,4761053,4761054,4761598,4761599,4761603,4761739,77227565,77227566,701156,701157,701158,5258478,19039057,19039058,19039059,701159,701160,19039060,19039062,701162,701163,701164,3768972,3768973,19722218,19722219,19722220,19722222,19722223,4659547,3881146,3881147,701165,701166,701167,701172,701176,701180,18714471,18714472,18714473,19722414,19722415,19722417,19722419,19824647,20380636,2972903,701185,701186,701187,701188,701189,701190,701191,701192,701195,701196,701199,701204,701205,701208,701213,701214,701219,701220,701223,2929812,2929816,2929819,701224,701226,701227,701229,701230,2972639,2972642,701231,2972647,2972650,701232,2972659,701233,701234,701235,3055909,701236,3055912,701237,3055915,701238,701239,701240,3055920,701241,3055923,2972744,701242,701243,701244,701245,701251,701257,701259,701260,701261,701264,701269,701274,2972776,77208516,77208520,701280,701282,2972783,2972784,701286,2972789,2972791,2972792,701294,701296,701300,701301,701306,701307,701312,2972804,2972806,2972821,2972823,2972825,701317,701318,5258268,701327,701328,701329,701331,701336,701339,701344,701347,2972831,2972846,701353,701354,701355,3781900,2972854,2972858,701359,701364,3772423,54989090,701371,701372,701374,3055966,3055967,3055972,3055974,701378,701385,701386,701390,701391,701392,701394,701396,701398,701400,701401,701403,701406,701412,701415,701418,701420,2972868,701429,701432,701433,701434,701435,701438,701440,701441,701444,701446,701449,701451,701454,701463,701465,701468,701469,701472,3772418,701479,701484,701487,701490,701493,701494,3931382,701497,701498,701505,701506,701510,701514,701520,701523,701524,701530,701540,701543,701546,701555,701558,701562,701568,701569,701570,701571,701574,701575,701576,701580,701584,701585,2972883,2972887,2972888,2972891,2972894,2972897,19118012,19118016,701588,701589,701590,701591,701592,701608,701609,701618,701619,701622,701624,701630,701633,701643,701737,701738,701740,701745,701749,701750,701751,701752,701755,701757,20237330,701759,701760,701767,701769,701771,701773,2971982,701776,701777,701779,701781,701784,701785,701787,701791,701794,16166971,16166973,16166975,704796,704798,704799,704800,704807,2978945,704815,18223315,704818,20385155,704808,704809,704831,704975,704977,704990,705003,705005,705009,705016,705021,705028,705030,705033,705036,705045,705050,705062,18403427,18403426,18408523,18408522,705064,705065,705067,705068,705070,705077,705078,705080,705082,705084,19788633,705086,705088,19773509,705090,705092,19806535,705094,705097,705109,705112,705114,19196676,19196677,705063,705102,705104,705125,705146,705149,705155,705161,701824,701825,701826,701827,2970832,3931395,5226694,2970833,3931396,5228177,4613658,5227899,701829,701830,701832,701834,701838,701845,701847,18951672,701849,701850,701851,701852,2970861,20156506,2970862,20156502,701853,701859,19072535,701865,701867,701881,701882,701883,701884,701885,701886,701889,701892,701894,701896,701905,701906,3772431,16166981,701908,701909,701911,701913,701915,701916,701918,701919,701922,701925,701927,701929,701932,701935,701939,701941,701945,701946,701949,701952,701954,701956,701959,701962,701967,701976,701977,701979,16166986,16166987,16166988,16166993,16166998,701982,701983,702000,702001,702003,702005,702007,702009,702011,702013,702015,702017,702018,702019,702020,702023,702025,702028,3783028,19197204,702036,702038,702042,702044,702047,702048,19216507,702059,702060,702067,702068,702071,702072,702073,702081,702083,702085,702087,702089,702091,702093,702095,702099,18009824,18009825,18009826,702103,702104,702105,702108,702115,702122,18816204,702149,2712661,702156,702158,702160,702170,702171,2972568,3769083,3769087,18705468,20427305,3769088,3769089,3769090,3769091,3782592,16199085,77237365,77218769,702173,702174,702175,702176,702177,702178,702179,702180,702182,702183,702184,702185,702187,20650630,702181,702188,702189,702190,702191,702192,20650631,702193,702194,702195,702196,702197,702198,702204,702205,16176706,702210,702212,702221,702223,702227,702228,702229,702231,702233,702235,702237,702241,702243,702245,702247,702249,702251,702263,2972339,702279,702301,19004444,702304,702305,702307,20463194,20463196,20463198,20463273,702320,702334,77209070,3781746,5464419,5464420,3781748,5464421,5464424,19795851,19795852,19795857,702337,702338,702339,702342,702343,702345,702346,702348,702350,702354,702356,702357,702359,702364,702370,702377,702378,702385,702386,702387,702390,702393,702395,3056092,702398,702400,3056095,3056097,702402,3056103,702407,702413,702415,702421,702430,702433,702435,702437,702438,702441,702442,16168671,702444,702445,702447,702449,702452,702458,702463,702464,702469,702471,702473,702478,702484,2972244,2972256,2972275,2972278,2972280,2972282,3056106,2972283,3056109,3056111,3056117,2972292,2972306,3056086,3056088,3056089,19021091,19021093,702486,702487,702488,702489,702492,702495,702498,702501,702502,702508,702523,702524,702525,702530,702531,702533,702535,702537,702539,18805322,702542,702543,2709798,702545,702550,702551,702554,702556,702557,702558,702563,702564,702565,702566,702567,702569,702570,702572,702573,702575,702578,702579,702580,702581,702582,702586,702587,702589,18805317,702593,702595,702597,702598,702600,702601,702605,702608,702609,702611,3772330,2987774,2987775,2987776,2987779,702613,702614,702617,702618,702620,702622,702623,2972071,2972072,19767559,19767561,19767563,19767567,19767571,19767572,19767577,19788637,19788639,19790702,19790704,19767581,19767583,19767585,19767589,19767592,19767593,19767598,702625,702626,702627,702630,702636,702638,702639,702640,702645,702651,702652,702654,702656,702657,702659,702661,702663,702665,702667,702671,702672,702687,702689,702692,702695,702696,702700,702702,702705,702707,2709800,702714,702715,702718,3772185,2972902,702720,4019892,17901747,18787580,18787696,18794471,18798867,18798870,20169908,19767552,19767554,702722,702723,702724,702725,702728,18124333,18197480,20386259,702731,702732,702733,4019894,20288865,17901743,17901745,17901746,18197481,77131548,2992289,2992293,702735,702736,702737,702742,3772320,3772321,702748,702752,702755,702762,16174986,702763,702768,702774,702780,702783,702784,702787,702790,702792,702794,702797,702798,702799,702800,702803,702806,702808,702809,702812,702813,702816,4244620,4244622,702819,702820,18050889,702824,702827,702828,702829,77212616,702832,702833,702834,702835,702837,20558066,702839,702840,702846,702851,702867,2929654,20559428,702878,702881,702882,702893,702894,702897,702899,702901,702903,702906,15727160,702909,702914,702916,702920,702923,702925,702927,702929,702934,702935,702942,702943,702944,702948,702955,702961,702962,702964,702966,702968,702970,702971,702972,702974,702976,19073114,702979,702980,702985,702988,702990,702992,702995,702997,702998,703000,703003,2931489,2931490,703009,703010,703013,703017,703018,18935542,18935543,18935547,20528551,77236012,77236014,77236015,703022,703023,703024,703038,703043,703044,703045,703052,703055,703058,19073956,19073959,703061,703075,703076,703083,2972365,703086,703087,703093,703094,703096,703099,703101,703111,703116,2972372,703117,703118,703119,703120,703121,703127,20462696,703134,703143,703144,703145,703146,77208144,77208145,77208146,703149,703150,703154,703156,77208147,77208148,703160,703161,703166,703171,703174,703175,703176,703178,703181,703182,703183,703185,703187,703189,703190,703192,703193,703201,703202,703203,703204,703208,703209,703212,703219,703223,703226,703229,703237,3769099,703241,703242,703243,703245,703249,703252,703254,703256,703258,2929689,2929690,2929697,2929699,2929701,3769075,2973009,2973010,2973011,2973012,2973015,2973018,2973022,2973023,2973024,2973027,703644,703645,703646,703649,703652,703655,703661,703663,18935519,19096206,20169746,703670,19095775,19767664,703672,19096214,703677,703682,703686,703688,703689,703691,703693,703696,703697,703702,703706,703714,703725,703726,703729,703730,703731,703736,703737,18814801,701650,701651,701657,701665,701668,701670,701673,2987786,701677,2987793,2987795,701680,701681,701691,701696,701704,701705,701710,701716,701717,701719,701720,701723,701725,2986541,701730,701733,77130988,701797,701806,701812,701798,701801,2970926,701820,4598924,701822,77130960,77130980,3772434,3772435,4598912,3772438,3772445,3772446,3772436,3772437,20378563,702124,702125,16173259,702126,2972923,2972937,702131,2972914,2972918,2972925,3026895,2972926,3026903,2972928,2972932,3772441,3772442,702140,702141,702142,702146,77130670,77130671,77130772,77130780,77130787,77130788,77130791,77130833,77130852,77130857,77130891,77130892,703743,703744,703745,703746,703748,703750,703752,703754,703756,703757,703761,703765,703768,703772,703775,703778,703781,703784,703787,703790,703793,703796,703800,703801,703803,703805,703806,703828,703829,703830,703835,703837,703838,703839,703844,703846,703847,703849,703851,703856,703857,703859,703860,703863,703864,703867,703868,703872,703874,2929754,703891,703904,703905,703907,703908,3772451,3772452,3772453,3772542,3772546,703910,703911,703912,703913,703916,703929,703933,2986097,703934,703935,703938,703942,703945,703948,703952,703953,703954,703955,703967,703973,703975,703978,703984,703986,703987,703995,703997,704000,704002,704004,704006,704009,704010,704020,704029,704031,704032,704038,704040,704043,704045,704048,704051,704052,704056,704061,2978899,704064,704067,704068,704072,704073,704081,704084,704087,704088,704094,704103,704104,704105,704107,704109,704111,77227573,2927785,704116,704118,704121,704123,704124,704132,704138,704141,704142,704146,704148,77227577,77227580,704150,704152,704154,704157,704161,77227583,77227586,15724827,77227589,77227592,704164,704166,704168,704170,704172,15724829,704175,704177,704179,704181,704183,704186,704187,704189,704191,704193,704196,15724837,704199,704200,704203,704210,704211,704212,2972518,2972528,2972732,704226,704227,2972601,2972602,2972611,2972615,2972733,2972623,2972706,19767422,19767425,19767428,19767430,19767432,704288,704289,704290,704291,704292,704293,704294,704301,704304,704306,16144967,16153457,704309,704310,704312,704314,704315,704317,704320,704323,704336,704338,704410,704411,704412,704415,704418,704421,704425,704432,704436,704444,704445,704446,704447,704452,704455,704458,704459,704460,704461,704464,704465,704466,704467,704471,704472,704473,20562067,20562068,704477,704478,704479,704490,704491,704492,704494,704496,704497,2928347,2928349,2928355,2928357,704505,704509,704514,704519,704524,704525,704526,20562440,3056675,3056677,704531,704532,704537,704538,77235346,704541,704548,704555,704556,704559,2971924,704561,704570,704578,704579,704582,2971928,704584,704585,704586,704591,704592,704596,704599,2971931,704601,704604,704605,704608,704610,704612,704613,704614,704618,2928366,704622,704626,704627,704628,704629,704638,704642,704644,704646,704647,704648,704668,704701,704702,704707,704709,704713,704714,704716,704717,704719,704720,704722,704723,704724,704726,704737,704740,704742,16172595,704744,704745,704754,704764,704765,704766,704767,19119488,19119489,19119491,19119492,19119494,19119496,19119498,19119500,19119502,19119504,19119506,19119508,19119510,19119512,19119514,19119516,19119518,19119520,19119522,20372959,20372960,20372961,20372962,20372964,20372968,20372970,20372972,20372974,20372979,20372981,20372983,20372985,20372987,20372989,20372991,20372993,20372995,20372997,20372999,20373001,20373003,20373005,20373007,20373009,20373011,20373013,20373015,55021131,20373017,20373019,20373021,20373023,20373025,20373027,20373029,20373031,20373033,20373035,20373037,20373039,20373041,20521295,77216134,77216136,20169712,20169713,20237772,20237773,20455811,77141069,20455815,77141071,20455819,20455820,20574140,20350766,20350767,20350768,20350769,20350770,20350771,20350772,20350773,20350774,20350775,20350776,20350777,20350778,20169717,20237785,20237786,20237787,20169725,20169726,20169737,20169738,20285067,20285068,77212713,704258,704259,704260,704263,704345,704346,704350,704351,704352,704353,2972721,2972725,704385,704391,704394,704395,704397,704399,704401,704406,704408,704362,704363,2972754,2972755,2972760,2972765,2972769,2972796,77212729,77212730,77237755,3027090,3027091,3027096,3027099,19119524,20426531,3027100,3027102,3993515,16168673,16168674,16169440,16169443,16169447,16169451,16169454,3027103,3782609";
        String limit = "10000";
        String start = "0";
        String IDX = "1";
        String checkAll = "0";
        String node = "-1";
    }
}
