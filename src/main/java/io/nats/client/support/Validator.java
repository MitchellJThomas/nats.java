// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client.support;

import io.nats.client.api.ConsumerConfiguration;

import java.time.Duration;

import static io.nats.client.JetStreamSubscription.MAX_PULL_SIZE;

public abstract class Validator {
    private Validator() {} /* ensures cannot be constructed */

    public static String validateMessageSubjectRequired(String s) {
        if (nullOrEmpty(s)) {
            throw new IllegalArgumentException("Subject cannot be null or empty [" + s + "]");
        }
        return s;
    }

    public static String validateJsSubscribeSubjectRequired(String s) {
        if (nullOrEmpty(s) || containsWhitespace(s)) {
            throw new IllegalArgumentException("Subject cannot have whitespace if provided [" + s + "]");
        }
        return s;
    }

    public static String validateQueueNameRequired(String s) {
        if (nullOrEmpty(s) ||containsWhitespace(s)) {
            throw new IllegalArgumentException("Queue have whitespace [" + s + "]");
        }
        return emptyAsNull(s);
    }

    public static String validateReplyToNullButNotEmpty(String s) {
        if (notNullButEmpty(s)) {
            throw new IllegalArgumentException("ReplyTo cannot be blank when provided [" + s + "]");
        }
        return s;
    }

    public static String validateStreamName(String s) {
        if (containsDotWildGt(s)) {
            throw new IllegalArgumentException("Stream cannot contain a '.', '*' or '>' [" + s + "]");
        }
        return s;
    }

    public static String validateStreamNameOrEmptyAsNull(String s) {
        return emptyAsNull(validateStreamName(s));
    }

    public static String validateStreamNameRequired(String s) {
        if (nullOrEmpty(s)) {
            throw new IllegalArgumentException("Stream cannot be null or empty and cannot contain a '.', '*' or '>' [" + s + "]");
        }
        return validateStreamName(s);
    }

    public static String validateDurableOrEmptyAsNull(String s) {
        if (containsDotWildGt(s)) {
            throw new IllegalArgumentException("Durable cannot contain a '.', '*' or '>' [" + s + "]");
        }
        return emptyAsNull(s);
    }

    public static String validateDurableRequired(String durable, ConsumerConfiguration cc) {
        if (durable == null) {
            if (cc == null) {
                throw new IllegalArgumentException("Durable is required and cannot contain a '.', '*' or '>' [null]");
            }
            return validateDurableRequired(cc.getDurable());
        }
        return validateDurableRequired(durable);
    }

    public static String validateDurableRequired(String s) {
        if (nullOrEmpty(s) || containsDotWildGt(s)) {
            throw new IllegalArgumentException("Durable is required and cannot contain a '.', '*' or '>' [" + s + "]");
        }
        return s;
    }

    public static int validatePullBatchSize(int pullBatchSize) {
        if (pullBatchSize < 1 || pullBatchSize > MAX_PULL_SIZE) {
            throw new IllegalArgumentException("Pull Batch Size must be between 1 and " + MAX_PULL_SIZE + " inclusive [" + pullBatchSize + "]");
        }
        return pullBatchSize;
    }

    public static long validateMaxConsumers(long max) {
        return validateGtZeroOrMinus1(max, "Max Consumers");
    }

    public static long validateMaxMessages(long max) {
        return validateGtZeroOrMinus1(max, "Max Messages");
    }

    public static long validateMaxBytes(long max) {
        return validateGtZeroOrMinus1(max, "Max Bytes");
    }

    public static long validateMaxMessageSize(long max) {
        return validateGtZeroOrMinus1(max, "Max message size");
    }

    public static int validateNumberOfReplicas(int replicas) {
        if (replicas < 1 || replicas > 5) {
            throw new IllegalArgumentException("Replicas must be from 1 to 5 inclusive.");
        }
        return replicas;
    }

    public static Duration validateDurationRequired(Duration d) {
        if (d == null || d.isZero() || d.isNegative()) {
            throw new IllegalArgumentException("Duration required and must be greater than 0.");
        }
        return d;
    }

    public static Duration validateDurationNotRequiredGtOrEqZero(Duration d) {
        if (d == null) {
            return Duration.ZERO;
        }
        if (d.isNegative()) {
            throw new IllegalArgumentException("Duration must be greater than or equal to 0.");
        }
        return d;
    }

    public static String validateJetStreamPrefix(String s) {
        if (containsWildGtDollarSpaceTab(s)) {
            throw new IllegalArgumentException("Prefix cannot contain a wildcard or dollar sign [" + s + "]");
        }
        return s;
    }

    public static Object validateNotNull(Object o, String fieldName) {
        if (o == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        return o;
    }

    public static String validateNotNull(String s, String fieldName) {
        if (s == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        return s;
    }

    public static long validateGtZeroOrMinus1(long l, String label) {
        if (zeroOrLtMinus1(l)) {
            throw new IllegalArgumentException(label + " must be greater than zero or -1 for unlimited");
        }
        return l;
    }

    // ----------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------
    public static boolean nullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean notNullButEmpty(String s) {
        return s != null && s.length() == 0;
    }

    public static boolean containsWhitespace(String s) {
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                if (Character.isWhitespace(s.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsDotWildGt(String s) {
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                switch (s.charAt(i)) {
                    case '.':
                    case '*':
                    case '>':
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean containsWildGtDollarSpaceTab(String s) {
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                switch (s.charAt(i)) {
                    case '*':
                    case '>':
                    case '$':
                    case ' ':
                    case '\t':
                        return true;
                }
            }
        }
        return false;
    }

    public static String emptyAsNull(String s) {
        return nullOrEmpty(s) ? null : s;
    }

    public static boolean zeroOrLtMinus1(long l) {
        return l == 0 || l < -1;
    }
}
