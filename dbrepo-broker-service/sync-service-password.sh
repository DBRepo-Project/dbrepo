#!/bin/bash
set -euo pipefail

: "${RABBITMQ_USERNAME:?Broker service username is required}"
: "${RABBITMQ_PASSWORD:?Broker service password is required}"
. /opt/bitnami/scripts/librabbitmq.sh
. /opt/bitnami/scripts/rabbitmq-env.sh

if [[ ${1:-} == --check ]]; then
    # Bitnami's setup server is temporary; wait for the final foreground process.
    case "$(cat /proc/1/comm)" in rabbitmq-server|beam.smp) ;; *) exit 1 ;; esac
    rabbitmq-diagnostics -q check_running >/dev/null 2>&1 || exit 1
    # Read inside the node so the password never appears in CLI arguments or output.
    if ! rabbitmqctl -q eval 'case rabbit_auth_backend_internal:user_login_authentication(unicode:characters_to_binary(os:getenv("RABBITMQ_USERNAME")), [{password, unicode:characters_to_binary(os:getenv("RABBITMQ_PASSWORD"))}]) of {ok, _} -> ok; _ -> erlang:error(broker_credentials_not_ready) end.' >/dev/null 2>&1; then
        exit 1
    fi
    exit 0
fi

# Render before Bitnami setup: it imports definitions again when the final server starts.
umask 077
if ! ERL_CRASH_DUMP=/dev/null erl +S 1:1 +SDcpu 1 +SDio 1 -noshell -pa /opt/bitnami/rabbitmq/plugins/*/ebin -eval '
    {ok, Input} = file:read_file("/app/definitions.json"),
    Definitions = rabbit_json:decode(Input),
    Username = unicode:characters_to_binary(os:getenv("RABBITMQ_USERNAME")),
    Password = unicode:characters_to_binary(os:getenv("RABBITMQ_PASSWORD")),
    Users = maps:get(<<"users">>, Definitions),
    1 = length([U || U <- Users, maps:get(<<"name">>, U) =:= Username]),
    Hash = base64:encode(rabbit_password:hash(Password)),
    Updated = [case maps:get(<<"name">>, U) =:= Username of
        true -> (maps:remove(<<"password">>, U))#{<<"password_hash">> => Hash,
                  <<"hashing_algorithm">> => <<"rabbit_password_hashing_sha256">>};
        false -> U
    end || U <- Users],
    ok = file:write_file("/tmp/dbrepo-definitions.json", rabbit_json:encode(Definitions#{<<"users">> => Updated})),
    halt().
' >/dev/null 2>&1; then
    echo 'Failed to prepare broker service credentials' >&2
    exit 1
fi
exec /opt/bitnami/scripts/rabbitmq/entrypoint.sh "$@"
