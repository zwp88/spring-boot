/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.quartz.autoconfigure;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Quartz Scheduler integration.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.quartz")
public class QuartzProperties {

	/**
	 * Quartz job store type.
	 */
	private JobStoreType jobStoreType = JobStoreType.MEMORY;

	/**
	 * Name of the scheduler.
	 */
	private @Nullable String schedulerName;

	/**
	 * Whether to automatically start the scheduler after initialization.
	 */
	private boolean autoStartup = true;

	/**
	 * Delay after which the scheduler is started once initialization completes. Setting
	 * this property makes sense if no jobs should be run before the entire application
	 * has started up.
	 */
	private Duration startupDelay = Duration.ofSeconds(0);

	/**
	 * Whether to wait for running jobs to complete on shutdown.
	 */
	private boolean waitForJobsToCompleteOnShutdown = false;

	/**
	 * Whether configured jobs should overwrite existing job definitions.
	 */
	private boolean overwriteExistingJobs = false;

	/**
	 * Additional Quartz Scheduler properties.
	 */
	private final Map<String, String> properties = new HashMap<>();

	public JobStoreType getJobStoreType() {
		return this.jobStoreType;
	}

	public void setJobStoreType(JobStoreType jobStoreType) {
		this.jobStoreType = jobStoreType;
	}

	public @Nullable String getSchedulerName() {
		return this.schedulerName;
	}

	public void setSchedulerName(@Nullable String schedulerName) {
		this.schedulerName = schedulerName;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public Duration getStartupDelay() {
		return this.startupDelay;
	}

	public void setStartupDelay(Duration startupDelay) {
		this.startupDelay = startupDelay;
	}

	public boolean isWaitForJobsToCompleteOnShutdown() {
		return this.waitForJobsToCompleteOnShutdown;
	}

	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	public boolean isOverwriteExistingJobs() {
		return this.overwriteExistingJobs;
	}

	public void setOverwriteExistingJobs(boolean overwriteExistingJobs) {
		this.overwriteExistingJobs = overwriteExistingJobs;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

}
