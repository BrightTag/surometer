/*
 * Copyright 2014 Signal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.signal.surometer;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.suro.client.ConfigBasedLoadBalancerProvider;
import com.netflix.suro.client.ConfigBasedSuroClientProvider;
import com.netflix.suro.client.ISuroClient;
import com.netflix.suro.client.SyncSuroClient;
import com.netflix.suro.client.async.AsyncSuroClient;
import com.netflix.suro.connection.EurekaLoadBalancer;
import com.netflix.suro.connection.StaticLoadBalancer;

@Singleton
public class SuroSamplerModule extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<String, ILoadBalancer> loadBalancers = MapBinder.newMapBinder(binder(), String.class, ILoadBalancer.class);
        loadBalancers.addBinding("eureka").to(EurekaLoadBalancer.class);
        loadBalancers.addBinding("static").to(StaticLoadBalancer.class);

        MapBinder<String, ISuroClient> clientImpls = MapBinder.newMapBinder(binder(), String.class, ISuroClient.class);
        clientImpls.addBinding("async").to(AsyncSuroClient.class).in(LazySingletonScope.get());
        clientImpls.addBinding("sync").to(SyncSuroClient.class).in(LazySingletonScope.get());

        bind(ISuroClient.class).toProvider(ConfigBasedSuroClientProvider.class);
        bind(ILoadBalancer.class).toProvider(ConfigBasedLoadBalancerProvider.class);
    }

}