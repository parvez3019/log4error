package io.github.parvez3019.disruptor;

import com.lmax.disruptor.RingBuffer;

public class DelayedMultiEventProducer implements EventProducer {

    @Override
    public void startProducing(final RingBuffer<ValueEvent> ringBuffer, final int count) {
        final Runnable simpleProducer = () -> produce(ringBuffer, count, false);
        new Thread(simpleProducer).start();
    }

    private void produce(final RingBuffer<ValueEvent> ringBuffer, final int count, final boolean addDelay) {
        for (int i = 0; i < count; i++) {
            final long seq = ringBuffer.next();
            final ValueEvent valueEvent = ringBuffer.get(seq);
            valueEvent.setValue(i);
            ringBuffer.publish(seq);
        }
    }
}