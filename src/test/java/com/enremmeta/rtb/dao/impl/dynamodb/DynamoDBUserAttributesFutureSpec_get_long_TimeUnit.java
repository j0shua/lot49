package com.enremmeta.rtb.dao.impl.dynamodb;

import static com.enremmeta.rtb.dao.impl.dynamodb.DynamoDBDaoMapOfUserAttributes.ATTRIBUTES_EXPERIMENT_FIELD;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.enremmeta.rtb.api.UserAttributes;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*", "javax.management.*"})
public class DynamoDBUserAttributesFutureSpec_get_long_TimeUnit {
    private ExecutorService executor;
    private long maxTimeWaitTermination = 10000;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Before
    public void setUp() throws Exception {
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        try {
            executor.shutdown();
            executor.awaitTermination(maxTimeWaitTermination, TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void positiveFlow_returnsExpectedResultsIfFutureGetReturnsIt() throws Exception {
        long maxTimeWait = 10000;
        
        String key = "Key";
        String value = "Value";
        
        Map<String, String> expectedExperimentData = new HashMap<String, String>();
        expectedExperimentData.put(key, value);
        
        Map<String, AttributeValue> attributes = new HashMap<String, AttributeValue>();
        attributes.put(ATTRIBUTES_EXPERIMENT_FIELD, InternalUtils.toAttributeValue(expectedExperimentData));
        
        GetItemResult getItemResultMock = Mockito.mock(GetItemResult.class);
        Mockito.when(getItemResultMock.getItem()).thenReturn(attributes);

        Future<GetItemResult> future = executor.submit(() -> { return getItemResultMock; });
        
        DynamoDBUserAttributesFuture dynamoDBUserAttributesFuture = new DynamoDBUserAttributesFuture(future);
        
        UserAttributes result = dynamoDBUserAttributesFuture.get(maxTimeWait, TimeUnit.MILLISECONDS); /// act
        
        assertThat(result, not(equalTo(null)));
        assertThat(result.getUserExperimentData().getExperimentData().get(key), equalTo(value));
    }

    @Test
    public void negativeFlow_throwsTimeoutExceptionIfFutureGetWaitTimeElapsed() throws Exception {
        exceptionRule.expect(TimeoutException.class);
        
        long taskTimeout = 1000;
        long maxTimeWait = 10;
        
        Future<GetItemResult> future = executor.submit(() -> {
            TimeUnit.MILLISECONDS.sleep(taskTimeout);
            return Mockito.mock(GetItemResult.class);
        });
        
        DynamoDBUserAttributesFuture dynamoDBUserAttributesFuture = new DynamoDBUserAttributesFuture(future);
        
        dynamoDBUserAttributesFuture.get(maxTimeWait, TimeUnit.MILLISECONDS); /// act
    }
}
