package com.enremmeta.rtb.jersey;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.enremmeta.rtb.LogUtils;
import com.enremmeta.rtb.api.proto.openrtb.OpenRtbRequest;
import com.enremmeta.rtb.config.Lot49Config;
import com.enremmeta.rtb.proto.ExchangeAdapter;
import com.enremmeta.rtb.proto.brx.BrxAdapter;
import com.enremmeta.rtb.proto.brx.BrxRtb095;
import com.enremmeta.rtb.proto.brx.BrxRtb095.BidRequest.Ext;
import com.enremmeta.util.ServiceRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuctionsSvc.class, ServiceRunner.class, LogUtils.class,
                BrxRtb095.BidRequest.class})
@PowerMockIgnore({"javax.crypto.*", "javax.management.*"})
public class AuctionsSvcSpec_onBrx {
    // should process request from Ad Exchange
    private ServiceRunner serviceRunnerSimpleMock;
    private AuctionsSvc svc;
    private BrxAdapter brxAdapterMock;

    @Before
    public void setUp() throws Throwable {
        serviceRunnerSimpleMock = Mockito.mock(ServiceRunner.class, Mockito.CALLS_REAL_METHODS);

        brxAdapterMock = PowerMockito.mock(BrxAdapter.class);
        Mockito.when(brxAdapterMock.convertRequest(any(BrxRtb095.BidRequest.class)))
                        .thenCallRealMethod();

        PowerMockito.whenNew(BrxAdapter.class).withNoArguments().thenReturn(brxAdapterMock);

        Lot49Config configMock = Mockito.mock(Lot49Config.class);

        PowerMockito.mockStatic(ServiceRunner.class);
        Mockito.when(ServiceRunner.getInstance()).thenReturn(serviceRunnerSimpleMock);
        Mockito.when(serviceRunnerSimpleMock.getConfig()).thenReturn(configMock);

        svc = new AuctionsSvc();

        PowerMockito.mockStatic(AuctionsSvc.class);
        PowerMockito.doNothing().when(AuctionsSvc.class);
        AuctionsSvc.onBidRequestDelegate(any(JerseySvc.class), anyString(),
                        any(ExchangeAdapter.class), any(AsyncResponse.class),
                        any(OpenRtbRequest.class), anyString(), any(HttpServletRequest.class),
                        anyString());
    }

    @Test
    public void positiveFlow_shouldConfiureBrxAdapterAndCallOnBidRequestDelegate()
                    throws Throwable {
        BrxRtb095.BidRequest req = BrxRtb095.BidRequest.getDefaultInstance();
        Ext ext = Ext.getDefaultInstance();
        Whitebox.setInternalState(ext, "isPing_", false);
        Whitebox.setInternalState(req, "ext_", ext);

        svc.onBrx(Mockito.mock(AsyncResponse.class), Mockito.mock(UriInfo.class), req, "xff",
                        Mockito.mock(HttpServletRequest.class), "xrip", "debug");

        // verify adapter constructor call
        PowerMockito.verifyNew(BrxAdapter.class).withNoArguments();

        // verify adapter call to convertRequest
        Mockito.verify(brxAdapterMock, times(1)).convertRequest(any(BrxRtb095.BidRequest.class));

        PowerMockito.verifyStatic(Mockito.times(1));
        AuctionsSvc.onBidRequestDelegate(any(JerseySvc.class), anyString(),
                        any(ExchangeAdapter.class), any(AsyncResponse.class),
                        any(OpenRtbRequest.class), anyString(), any(HttpServletRequest.class),
                        anyString());

    }

    @Test
    public void negativeFlow_shouldLogBrockenRequest() throws Exception {


        // BidRequest is broken now
        BrxRtb095.BidRequest badReq = PowerMockito.mock(BrxRtb095.BidRequest.class);

        PowerMockito.mockStatic(LogUtils.class);
        PowerMockito.doNothing().when(LogUtils.class);
        LogUtils.error(anyString());


        svc.onBrx(Mockito.mock(AsyncResponse.class), Mockito.mock(UriInfo.class), badReq, "xff",
                        Mockito.mock(HttpServletRequest.class), "xrip", "debug");

        // verify adapter constructor call
        PowerMockito.verifyNew(BrxAdapter.class).withNoArguments();

        // but onBidRequestDelegate shouldn't be called due exception
        PowerMockito.verifyStatic(Mockito.never());
        AuctionsSvc.onBidRequestDelegate(any(JerseySvc.class), anyString(),
                        any(ExchangeAdapter.class), any(AsyncResponse.class),
                        any(OpenRtbRequest.class), anyString(), any(HttpServletRequest.class),
                        anyString());

        // verify exception
        PowerMockito.verifyStatic(Mockito.times(1));
        LogUtils.error(Mockito.eq("Error parsing request from BrX: null"),
                        Mockito.any(NullPointerException.class));
    }

    @Test
    public void positiveFlow_brXPing() throws Exception {

        BrxRtb095.BidRequest req = BrxRtb095.BidRequest.getDefaultInstance();
        Ext ext = Ext.getDefaultInstance();
        Whitebox.setInternalState(ext, "isPing_", true);
        Whitebox.setInternalState(req, "ext_", ext);


        PowerMockito.mockStatic(LogUtils.class);
        PowerMockito.doNothing().when(LogUtils.class);
        LogUtils.info("Replying to ping from BRX");


        svc.onBrx(Mockito.mock(AsyncResponse.class), Mockito.mock(UriInfo.class), req, "xff",
                        Mockito.mock(HttpServletRequest.class), "xrip", "debug");

        // verify adapter constructor call
        PowerMockito.verifyNew(BrxAdapter.class, Mockito.never()).withNoArguments();

        // but onBidRequestDelegate shouldn't be called due exception
        PowerMockito.verifyStatic(Mockito.never());
        AuctionsSvc.onBidRequestDelegate(any(JerseySvc.class), anyString(),
                        any(ExchangeAdapter.class), any(AsyncResponse.class),
                        any(OpenRtbRequest.class), anyString(), any(HttpServletRequest.class),
                        anyString());

        // verify exception
        PowerMockito.verifyStatic(Mockito.times(1));
        LogUtils.info("Replying to ping from BRX");
    }

}
