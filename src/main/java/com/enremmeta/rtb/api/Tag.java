package com.enremmeta.rtb.api;

import java.net.URLEncoder;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import com.enremmeta.rtb.api.proto.openrtb.Bid;
import com.enremmeta.rtb.api.proto.openrtb.Impression;
import com.enremmeta.rtb.api.proto.openrtb.OpenRtbRequest;
import com.enremmeta.rtb.api.proto.openrtb.lot49.Lot49Ext;
import com.enremmeta.rtb.config.Lot49Config;
import com.enremmeta.rtb.constants.Lot49Constants;
import com.enremmeta.rtb.constants.Macros;
import com.enremmeta.rtb.jersey.StatsSvc;
import com.enremmeta.rtb.proto.ExchangeAdapter;

/**
 * Specifies configuration of a "tag".
 * <p>
 * To keep the terminology straight, we define a "tag" to comprise two things:
 * <ul>
 * <li><b>Web tag</b> - this is really what most users would think of. It is run in the browser - a
 * piece of HTML/JS/VAST, etc. It is probably generated by an ad server.</li>
 * <li><b>Bid tag</b> - a class implementing current interface. It is a piece of code run in the
 * builder that is responsible for the following major functions (with default implementation
 * provided by methods in {@link TagImpl}):
 * <table border=1>
 * <caption> </caption>
 * <tr>
 * <th>Functionality</th>
 * <th>Where implemented</th>
 * </tr>
 * <tr>
 * <td>Holding the parameters of the web tag and creative (e.g., MIME types, dimensions, etc.)</td>
 * <td>Various fields.</td>
 * </tr>
 * <tr>
 * <td>Checking whether the specified impression request is a match</td>
 * <td>{@link #canBid(OpenRtbRequest, Impression)}</td>
 * </tr>
 * <tr>
 * <td>Generating "web tag" to be sent to the browser.</td>
 * <td>{@link #getTag(OpenRtbRequest, Impression, Bid, boolean)}</td>
 * </tr>
 * <tr>
 * <td>Generating {@link Bid} bid to be sent in the response to the bid request, containing the
 * mentioned web tag {@link Bid#getAdm() as markup} or as a {@link Bid#getNurl() reference}</td>
 * <td>{@link #getBid(OpenRtbRequest, Impression)}</td>
 * </tr>
 * </table>
 * <p>
 * 
 * Thus, in the simplest case, the author of this bid tag just needs to fill in the required fields
 * and override {@link #getTagTemplate(OpenRtbRequest, Impression, Bid)}. However, for more advanced
 * functionality, any method can be overridden.
 * </ul>
 * <p>
 * The naming convention for class names (and, therefore, filenames) to name them
 * <tt>Tag_&lt;TagID&gt;_&lt;TagName&gt;_&lt;AdId&gt;_&lt;AdName&gt;</tt> where
 * <ul>
 * <li></li>
 * </ul>
 * 
 * @see Ad
 * 
 * @author Gregory Golberg (grisha@alum.mit.edu)
 * 
 *         Copyright © <a href="http://www.enremmeta.com">Enremmeta LLC</a> 2014. All Rights
 *         Reserved. This code is licensed under
 *         <a href="http://www.gnu.org/licenses/agpl-3.0.html">Affero GPL 3.0</a>
 *
 */
public interface Tag extends Lot49Plugin {

    public Dimension getDimension();

    public boolean isBanner();

    public boolean isVideo();

    public String getMime();

    public List<String> getMimes();

    public boolean isLinear();

    public boolean isVpaid();

    /**
     * Can this tag be served in response to the provided impression? If null is returned, the
     * answer is yes, otherwise, the string with a reason of why not is returned (for use in logging
     * and troubleshooting).
     * <p>
     * </p>
     * This will always return <tt>null</tt> if <tt>req</tt> {@link Lot49Ext#isTest() is a test
     * request}.
     * 
     * @param req
     *            parent request
     * 
     * @param imp
     *            impression to evaluate
     */
    public String canBid(OpenRtbRequest req, Impression imp);

    /**
     * Duration of this ad if it is a video ad.
     */
    int getDuration();


    public SortedMap<String, SortedMap<String, AtomicLong>> getOptoutsByExchange();

    String getTagVersion();


    void incrOptout(OpenRtbRequest req, String name);

    public long getImpressionsConsidered(String exchange);

    /**
     * Cachebuster.
     */
    public String getCb();

    public String getProto(OpenRtbRequest req);

    public boolean isSslCapable();

    public MarkupType getMarkupType();

    /**
     * Human-readable description
     */
    String getDesc();

    /**
     * Custom impression pass-through data.
     * 
     * @return
     */
    String getCustomImpPassThruData();

    /**
     * ID of this tag.
     */
    String getId();

    /**
     * Generates a link to the Lot49 impression tracker.
     * <p>
     * In case of a {@link ExchangeAdapter#isNurlRequired() non-NUrl-requiring} exchange, this URL
     * will include a <tt>wp</tt> query parameter whose value is the
     * {@link ExchangeAdapter#getWinningPriceMacro() exchange-specific winning price macro}.
     * </p>
     * If <tt>{@link #getImpRedir() impRedir}</tt> is not <tt>null</tt>, this URL will include
     * <tt>r</tt> query parameter whose value is {@link #encode(String) URL-encoded}
     * <tt>impRedir</tt>.
     * 
     * @see Lot49Constants#DEFAULT_CLICK_PATH_ABSOLUTE
     * 
     * @see Lot49Config#getStatsUrl()
     * 
     * @see StatsSvc#click(UriInfo, String, String, String, String, String, String, String, String,
     *      String, String, String, String, HttpServletRequest, String, String)
     */
    String getImpressionUrl(OpenRtbRequest req, Bid bid, boolean nurl);

    /**
     * Generates a link to NUrl.
     * <p>
     * In case of a {@link ExchangeAdapter#isNurlRequired() NUrl-requiring} exchange, this URL will
     * include a <tt>wp</tt> query parameter whose value is the
     * {@link ExchangeAdapter#getWinningPriceMacro() exchange-specific winning price macro}.
     * </p>
     */
    String getNUrl(OpenRtbRequest req, Bid bid, String nurlId);

    int getApi();

    /**
     * Generates a link to the Lot49 click tracker. If
     * <tt>{@link #getClickRedir(OpenRtbRequest, Bid)
     * clickRedir }</tt> is not <tt>null</tt>, this URL will include <tt>r</tt> query parameter
     * whose value is {@link #encode(String) URL-encoded} <tt>clickRedir</tt>.
     * 
     * @see Lot49Constants#DEFAULT_CLICK_PATH_ABSOLUTE
     * 
     * @see Lot49Config#getStatsUrl()
     * 
     * @see StatsSvc#click(UriInfo, String, String, String, String, String, String, String, String,
     *      String, String, String, String, HttpServletRequest, String, String)
     */
    String getClickUrl(OpenRtbRequest req, Bid bid);

    String getImpRedir();

    Ad getAd();

    /**
     * Human-readable name.
     */
    String getName();

    /**
     * Get the actual tag to send in the {@link Bid#getAdm() ad markup} field of the bid (or in the
     * {@link Bid#getNurl() response to nurl}), based on
     * {@link #getTagTemplate(OpenRtbRequest, Impression, Bid) the template}.
     * <p>
     * The provider of the template needs to understand the mechanism of macro substitution. The
     * substitutions occur in the following order, each one going over the entire text of the
     * template.
     * </p>
     * <ol>
     * <li>No macro substitutions will be done to {@link #getClickRedir(OpenRtbRequest, Bid)
     * clickRedir} or {@link #getImpRedir() impRedir} fields. If necessary, override the method
     * appropriately.</li>
     * <li>All occurrences of {@link Macros#MACRO_LOT49_IMPRESSION} will be replaced by the
     * {@link #getImpressionUrl(OpenRtbRequest, Bid, boolean) URL to the Lot49 impression tracker}.
     * </li>
     * <li>TODO Proust</li>
     * <li>Unless {@link ExchangeAdapter#isMacrosInNurl() the exchange sends actual macros in a call
     * to NUrl}, the following click macro substitutions occur:
     * <ol>
     * <li>All occurrences of {@link Macros#MACRO_LOT49_CLICK_CHAIN_ENC} are replaced as follows:
     * <ul>
     * <li>If there is an {@link ExchangeAdapter#getClickMacro() exchange click macro}, then it is
     * replaced with {@link Macros#MACRO_LOT49_EXCHANGE_CLICK_ENC} followed by
     * {@link Macros#MACRO_LOT49_CLICK_ENC_ENC}.</li>
     * <li>If the exchange click macro does not exist, then this will be replaced by
     * {@link Macros#MACRO_LOT49_CLICK_ENC}
     * </ul>
     * <b>NOTE</b> The replacement macros, in turn, can be replaced in later passes.</li>
     * <li>All occurrences of {@link Macros#MACRO_LOT49_CLICK_ENC_ENC} will be replaced as
     * {@link Macros#MACRO_LOT49_CLICK} (see below), except the replacement will be doubly
     * URL-encoded.</li>
     * 
     * <li>All occurrences of {@link Macros#MACRO_LOT49_CLICK_ENC} will be replaced as
     * {@link Macros#MACRO_LOT49_CLICK} (see below), except the replacement will be URL-encoded.
     * </li>
     * 
     * <li>All occurrences of {@link Macros#MACRO_LOT49_CLICK} will be replaced by the URL to the
     * Lot49 click tracker. If <tt>clickRedir</tt> is not <tt>null</tt>, this URL will include
     * <tt>r</tt> query parameter whose value is {@link #encode(String) URL-encoded}
     * <tt>clickRedir</tt>.</li>
     * 
     * <li>All occurrences of {@link Macros#MACRO_LOT49_EXCHANGE_CLICK_ENC} will be replaced by the
     * {@link ExchangeAdapter#getClickEncMacro() exchange-specific version thereof}.</li>
     * <li>All occurrences of {@link Macros#MACRO_LOT49_EXCHANGE_CLICK} will be replaced by the
     * {@link ExchangeAdapter#getClickMacro() exchange-specific version thereof}.</li>
     * </ol>
     * <li>Otherwise, the values for {@link Macros#MACRO_LOT49_CLICK_ENC_ENC} ,
     * {@link Macros#MACRO_LOT49_CLICK_ENC} and {@link Macros#MACRO_LOT49_CLICK} are generated, but
     * not replaced. Instead, they are added to the NUrl, and will be substituted at
     * {@link StatsSvc#nurl(UriInfo, String, String, String, String, String, String, String, String, String, String, long, String, String, String, String, String, String, HttpServletRequest, String, String, String, String, String)
     * NUrl invocation}.
     * </ol>
     * 
     * @see ExchangeAdapter#isMacrosInNurl()
     * 
     * @see #getTagTemplate(OpenRtbRequest, Impression, Bid)
     * 
     * @see #getImpressionUrl(OpenRtbRequest, Bid, boolean)
     * 
     * @see #getClickUrl(OpenRtbRequest, Bid)
     * 
     * @see #getBid(OpenRtbRequest, Impression)
     */
    String getTag(OpenRtbRequest req, Impression imp, Bid bid, boolean nurl);

    /**
     * Tag template. This is pretty much the web tag that is intended to be sent to the browser, but
     * what makes it a template is the fact that it may be parameterized by the following:
     * <ol>
     * <li>Any of the {@link Tag fields} of the class (that is, anything that has a no-argument
     * getter, e.g. {@link #getMime()}.</li>
     * <li>Any of the local variables which are up to the author of the class overriding this method
     * to define and compute.</li>
     * <li>{@link Macros}. The difference of these from the others is that the above substitutions
     * are done during the invocation of this method, whereas the macros are substituted during the
     * actual {@link #getTag(OpenRtbRequest, Impression, Bid, boolean) tag generation}. It is at
     * that point, for example, that click and impression tracking URLs are constructed.
     * <p>
     * <b>IMPORTANT</b> It is up to the author of the implementing class to understand properly the
     * {@link #getTag(OpenRtbRequest, Impression, Bid, boolean) web tag generation order} and insert
     * macros appropriately and {@link #encode(String) encode} URLs as needed
     * </p>
     * </li>
     * </ol>
     * 
     * @see #getTag(OpenRtbRequest, Impression, Bid, boolean)
     * 
     * @see #getBid(OpenRtbRequest, Impression)
     */
    String getTagTemplate(OpenRtbRequest req, Impression imp, Bid bid);

    /**
     * URLEncode a string
     */
    @SuppressWarnings("deprecation")
    default String encode(String s) {
        return URLEncoder.encode(s);
    }

    List<String> validate();

    public int getProtocol();

    public void init();

    public ExchangeTargeting getExchangeTargeting();

    /**
     * Return the fully filled out {@link Bid} object, including the tag in the {@link Bid#getAdm()
     * ad markup} field.
     * 
     * @see #getTag(OpenRtbRequest, Impression, Bid, boolean)
     * 
     * @see #getTagTemplate(OpenRtbRequest, Impression, Bid)
     */
    Bid getBid(OpenRtbRequest req, Impression imp);

    public String getClickRedir(final OpenRtbRequest req, final Bid bid);

}
