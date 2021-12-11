FROM rust:1-slim-bullseye as builder

# first we build the rust backend

WORKDIR /app
COPY . .
RUN cargo install --debug --path .
RUN cargo clean # to reduce the image size

CMD slice-backend $PORT
