create type if not exists architecture_variant as enum (
  'monolith',
  'modular_monolith',
  'microservices'
);

create type if not exists benchmark_topology as enum (
  'single'
);

create type if not exists benchmark_status as enum (
  'running',
  'passed',
  'failed'
);

create type if not exists video_resolution as enum (
  'SD_360',
  'SD_480',
  'HD_720',
  'FHD_1080'
);

create table if not exists benchmark_runs (
  run_id text primary key,
  variant architecture_variant not null,
  topology benchmark_topology not null,
  scenario text not null,
  base_url text not null,
  video_file text not null,
  requested_resolutions video_resolution[] not null,
  status benchmark_status not null,
  gatling_exit_code integer,
  started_at timestamp not null default current_timestamp,
  finished_at timestamp,
  report_path text,
  notes text
);
